package caustic.runtime

import Database._

import org.apache.thrift.async.AsyncMethodCallback
import org.apache.thrift.TException

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * An asynchronous, transactional key-value store.
 */
trait Database extends thrift.Database.AsyncIface {

  /**
   * Asynchronously returns the latest revisions of the specified keys.
   *
   * @param keys Keys to lookup.
   * @param ec Implicit execution context.
   * @return Latest revisions of the specified keys.
   */
  def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, Revision]]

  /**
   * Asynchronously applies the specified changes if and only if the revisions of the specified
   * dependent keys remain their expected values and returns an exception on conflict. The
   * consistency, durability, and availability of the system depends on the implementation of this
   * conditional put operator.
   *
   * @param depends Version dependencies.
   * @param changes Updates.
   * @param ec Implicit execution context.
   * @return Future that completes when successful, or an exception otherwise.
   */
  def cput(depends: Map[Key, Version], changes: Map[Key, Revision])(
    implicit ec: ExecutionContext
  ): Future[Unit]

  /**
   * Asynchronously executes the specified transaction and returns the result. Transactions are
   * repeatedly partially evaluated until they are reduced to a literal value. Because all
   * transactions must terminate in literals (they are the "leaves" of the transaction) and all
   * expressions are reducible to literals, all transaction must eventually reduce to a literal.
   * Keys are read from the underlying database and are written to a local change buffer that is
   * conditionally put in the underlying database if and only if the read dependencies of the
   * transaction remain unchanged.
   *
   * @param expression Expression to execute.
   * @param ec Implicit execution context.
   * @return Result of transaction execution, or an exception on failure.
   */
  def execute(expression: Transaction)(
    implicit ec: ExecutionContext
  ): Future[Literal] = {
    val snapshot = mutable.Map.empty[Key, Revision]
    val buffer = mutable.Map.empty[Key, Revision]
    val locals = mutable.Map.empty[Key, Literal]

    def evaluate(txn: Transaction): Future[Literal] = {
      // Load all keys that are read (for their value) and written (for their version), that have
      // not been read before (to avoid changes in value and version) to ensure that the evaluation
      // of an expression is correct and consistent.
      @tailrec
      def rwset(stack: List[Transaction], aggregator: Set[Key]): Set[Key] = stack match {
        case Nil => aggregator
        case (_: Literal) :: rest => rwset(rest, aggregator)
        case Expression(Read, Text(key) :: _) :: rest => rwset(rest, aggregator + key)
        case Expression(Write, Text(key) :: _) :: rest => rwset(rest, aggregator + key)
        case (o: Expression) :: rest => rwset(o.operands ::: rest, aggregator)
      }

      val keys = rwset(List(txn), Set.empty) -- snapshot.keys
      snapshot ++= keys.map(k => k -> Revision.Initial)

      // Load the keys, update the local snapshot, and reduce the transaction. If the result is a
      // literal then return, otherwise recurse on the partially evaluated transaction.
      get(keys) flatMap { r =>
        snapshot ++= r
        reduce(List(txn), List.empty) match {
          case l: Literal => Future(l)
          case o: Expression => evaluate(o)
        }
      }
    }

    @tailrec
    def reduce(stack: List[Any], results: List[Transaction]): Transaction = (stack, results) match {
      // Return Results.
      case (Nil, _) =>
        if (results.size != 1)
          throw new thrift.ExecutionException(s"Transaction evaluates to $results.")
        else
          results.head

      // Replace Literals.
      case ((l: Literal) :: rest, rem) => reduce(rest, l :: rem)

      // Expand Expressions.
      case (Expression(Read, Text(k) :: Nil) :: rest, rem) =>
        reduce(rest, buffer.getOrElse(k, snapshot(k)).value :: rem)
      case (Expression(Write, Text(k) :: (v: Literal) :: Nil) :: rest, rem) =>
        buffer += k -> Revision(snapshot(k).version + 1, v)
        reduce(rest, v :: rem)
      case (Expression(Branch, cmp :: pass :: fail :: Nil) :: rest, rem) =>
        reduce(cmp :: Branch :: rest, pass :: fail :: rem)
      case (Expression(Cons, first :: second :: Nil) :: rest, rem) =>
        reduce(first :: Cons :: rest, second :: rem)
      case (Expression(Repeat, c :: b :: Nil) :: rest, rem) =>
        reduce(branch(c, cons(b, repeat(c, b)), text("")) :: rest, rem)
      case (Expression(Prefetch, Text(k) :: Nil) :: rest, rem) =>
        reduce(rest, k.split(",").map(x => read(Text(x))).reduceLeftOption(cons).getOrElse(text("")) :: rem)
      case ((e: Expression) :: rest, rem) =>
        reduce(e.operands.reverse ::: e.operator :: rest, rem)

      // Simplify Core Expressions.
      case (Read :: rest, k :: rem) => reduce(rest, read(k) :: rem)
      case (Write :: rest, k :: v :: rem) => reduce(rest, write(k, v) :: rem)
      case (Load :: rest, Text(k) :: rem) => reduce(rest, locals.getOrElse(k, text("")) :: rem)
      case (Load :: rest, k :: rem) => reduce(rest, load(k) :: rem)
      case (Store :: rest, Text(k) :: (v: Literal) :: rem) => locals += k -> v; reduce(rest, v :: rem)
      case (Store :: rest, k :: v :: rem) => reduce(rest, store(k, v) :: rem)
      case (Rollback :: _, (l: Literal) :: _) => throw RollbackException(l)
      case (Rollback :: rest, x :: rem) => reduce(rest, rollback(x) :: rem)
      case (Repeat :: rest, Flag(false) :: _ :: rem) => reduce(rest, rem)
      case (Repeat :: rest, c :: b :: rem) => reduce(rest, repeat(c, b) :: rem)
      case (Cons :: rest, f :: s :: rem) if f.isInstanceOf[Literal] => reduce(s :: rest, rem)
      case (Cons :: rest, f :: s :: rem) => reduce(rest, cons(f, s) :: rem)
      case (Branch :: rest, Flag(true) :: pass :: _ :: rem) => reduce(pass :: rest, rem)
      case (Branch :: rest, Flag(false) :: _ :: fail :: rem) => reduce(fail :: rest, rem)
      case (Branch :: rest, c :: p :: f :: rem) => reduce(rest, branch(c, p, f) :: rem)
      case (Prefetch :: rest, k :: rem) => reduce(rest, prefetch(k) :: rem)

      // Simplify String Expressions.
      case (Length :: rest, x :: rem) => reduce(rest, length(x) :: rem)
      case (Matches :: rest, x :: y :: rem) => reduce(rest, matches(x, y) :: rem)
      case (Contains :: rest, x :: y :: rem) => reduce(rest, contains(x, y) :: rem)
      case (Slice :: rest, x :: l :: h :: rem) => reduce(rest, slice(x, l, h) :: rem)
      case (IndexOf :: rest, x :: y :: rem) => reduce(rest, indexOf(x, y) :: rem)

      // Simplify Math Expressions.
      case (Add :: rest, x :: y :: rem) => reduce(rest, add(x, y) :: rem)
      case (Sub :: rest, x :: y :: rem) => reduce(rest, sub(x, y) :: rem)
      case (Mul :: rest, x :: y :: rem) => reduce(rest, mul(x, y) :: rem)
      case (Div :: rest, x :: y :: rem) => reduce(rest, div(x, y) :: rem)
      case (Mod :: rest, x :: y :: rem) => reduce(rest, mod(x, y) :: rem)
      case (Pow :: rest, x :: y :: rem) => reduce(rest, pow(x, y) :: rem)
      case (Log :: rest, x :: rem) => reduce(rest, log(x) :: rem)
      case (Sin :: rest, x :: rem) => reduce(rest, sin(x) :: rem)
      case (Cos :: rest, x :: rem) => reduce(rest, cos(x) :: rem)
      case (Floor :: rest, x :: rem) => reduce(rest, floor(x) :: rem)

      // Simplify Logical Expressions.
      case (Both :: rest, x :: y :: rem) => reduce(rest, both(x, y) :: rem)
      case (Either :: rest, x :: y :: rem) => reduce(rest, either(x, y) :: rem)
      case (Negate :: rest, x :: rem) => reduce(rest, negate(x) :: rem)

      // Simplify Comparison Expressions.
      case (Equal :: rest, x :: y :: rem) => reduce(rest, equal(x, y) :: rem)
      case (Less :: rest, x :: y :: rem) => reduce(rest, less(x, y) :: rem)

      // Default Error.
      case _ => throw new thrift.ExecutionException(s"$stack cannot be applied to $results.")
    }

    // Recursively reduce the transaction, and then conditionally persist all changes made by the
    // transaction to the underlying database if and only if the versions of its various
    // dependencies have not changed. Filter out empty first changes to allow local variables.
    evaluate(expression) recover {
      case e: RollbackException =>
        buffer.clear()
        e.result
    } flatMap { r =>
      cput(snapshot.mapValues(_.version).toMap, buffer.toMap).map(_ => r)
    }
  }

  override def execute(
    txn: thrift.Transaction,
    resultHandler: AsyncMethodCallback[thrift.Literal]
  ): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    // Execute the scala transaction and convert the result back to thrift.
    execute(Transaction.parse(txn)).onComplete {
      case Success(Real(r)) => resultHandler.onComplete(thrift.Literal.real(r))
      case Success(Flag(f)) => resultHandler.onComplete(thrift.Literal.flag(f))
      case Success(Text(t)) => resultHandler.onComplete(thrift.Literal.text(t))
      case Failure(e: TException) => resultHandler.onError(e)
      case Failure(e) => resultHandler.onError(new thrift.ExecutionException(s"Unknown error $e"))
    }
  }

}

object Database {

  /**
   * An exception that indicates that execution should terminate and the local change buffer should
   * be discarded. Effectively makes a transaction read-only.
   *
   * @param result Return value.
   */
  case class RollbackException(result: Literal) extends Exception

}