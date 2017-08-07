package caustic.runtime

import TransactionalDatabase._
import Operator._

import org.apache.thrift.async.AsyncMethodCallback

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.collection.mutable

/**
 * An asynchronous, transactional key-value store.
 */
trait TransactionalDatabase extends thrift.Database.AsyncIface {

  /**
   * Asynchronously returns the latest revisions of the specified keys.
   *
   * @param keys Keys to lookup.
   * @param ec Implicit execution context.
   * @return Latest of specified keys.
   */
  def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, Revision]]

  /**
   *
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
  def put(depends: Map[Key, Version], changes: Map[Key, Revision])(
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
   * @param transaction Transaction to execute.
   * @param ec Implicit execution context.
   * @return Result of transaction execution, or an exception on failure.
   */
  def execute(transaction: Transaction)(
    implicit ec: ExecutionContext
  ): Future[Literal] = {
    val snapshot = mutable.Map.empty[Key, Revision]
    val buffer   = mutable.Map.empty[Key, Revision]
    val locals   = mutable.Map.empty[Key, Literal]

    def evaluate(txn: Transaction): Future[Literal] = {
      // Load all keys in the transaction's readset (for their value) and in the transaction's
      // writeset (for their version) that have not been read before (to avoid changes in value) and
      // that are not current depended on by the transaction (to avoid changes in version) to ensure
      // that the evaluation of a transaction is correct and consistent.
      val keys = txn.readset ++ txn.writeset -- snapshot.keys
      snapshot ++= keys.map(k => k -> Revision(0L, Text("")))

      get(keys).transformWith {
        case Success(r) =>
          snapshot ++= r
          reduce(List(txn), List.empty) match {
            case l: Literal => Future(l)
            case e: Expression => evaluate(e)
          }
        case Failure(e) =>
          Future.failed(ReadException(e.getMessage))
      }
    }

    @tailrec
    def reduce(stack: List[Any], results: List[Transaction]): Transaction = (stack, results) match {
      // Return Results.
      case (Nil, _) =>
        if (results.size > 1)
          throw ExecutionException("Transaction evaluates to multiple results.")
        else
          results.head

      // Replace Literals.
      case ((l: Literal) :: rest, rem) =>
        reduce(rest, l :: rem)

      // Expand Expressions.
      case (Expression(READ, Text(key) :: Nil) :: rest, rem) =>
        val (_, value) = buffer.getOrElse(key, snapshot(key))
        reduce(rest, value :: rem)

      case (Expression(WRITE, Text(key) :: (value: Literal) :: Nil) :: rest, rem) =>
        buffer += key -> Revision(snapshot(key).version + 1, value)
        reduce(rest, rem)

      case (Expression(PREFETCH, Text(keys) :: Nil) :: rest, rem) =>
        val fetch = keys.split(",")
          .map(k => Expression(READ, Text(k) :: Nil))
          .reduceLeftOption((a, b) => Expression(CONS, a :: b :: Nil))
          .getOrElse(Text(""))
        reduce(rest, fetch :: rem)

      case (Expression(BRANCH, cmp :: pass :: fail :: Nil) :: rest, rem) =>
        reduce(cmp :: BRANCH :: rest, pass :: fail :: rem)

      case (Expression(CONS, first :: second :: Nil) :: rest, rem) =>
        reduce(first :: CONS :: rest, second :: rem)

      case (Expression(REPEAT, cond :: body :: Nil) :: rest, rem) =>
        val loop = Expression(CONS, body :: Expression(REPEAT, cond :: body :: Nil) :: Nil)
        reduce(Expression(BRANCH, cond :: loop :: Text("") :: Nil) :: rest, rem)

      case ((e: Expression) :: rest, rem) =>
        reduce(e.operands.reverse ::: e.operator :: rest, rem)

      // Core Operators.
      case (READ :: _, (l: Literal) :: _) if !l.isInstanceOf[Text] =>
        throw ExecutionException("Read key must be text.")
      case (READ :: rest, k :: rem) =>
        reduce(rest, Expression(READ, k :: Nil) :: rem)

      case (WRITE :: _, (l: Literal) :: _) if !l.isInstanceOf[Text] =>
        throw ExecutionException("Write key must be text.")
      case (WRITE :: rest, k :: v :: rem) =>
        reduce(rest, Expression(WRITE, k :: v :: Nil) :: rem)

      case (LOAD :: rest, Text(k) :: rem) if !locals.contains(k) =>
        throw ExecutionException(s"Load variable $k does not exist.")
      case (LOAD :: rest, Text(k) :: rem) =>
        reduce(rest, locals(k) :: rem)
      case (LOAD :: _, (l: Literal) :: _) if !l.isInstanceOf[Text] =>
        throw ExecutionException("Load variable must be text.")
      case (LOAD :: rest, k :: rem) =>
        reduce(rest, Expression(LOAD, k :: Nil) :: rem)

      case (STORE :: rest, Text(k) :: (v: Literal) :: rem) =>
        locals += k -> v
        reduce(rest, rem)
      case (STORE :: _, (l: Literal) :: _) if !l.isInstanceOf[Text] =>
        throw ExecutionException("Store variable must be text.")
      case (STORE :: rest, k :: v :: rem) =>
        reduce(rest, Expression(STORE, k :: v :: Nil) :: rem)

      case (ROLLBACK :: _, (l: Literal) :: _) =>
        throw RollbackException(l)
      case (ROLLBACK :: rest, x :: rem) =>
        reduce(rest, Expression(ROLLBACK, x :: Nil) :: rem)

      case (PREFETCH :: _, (l: Literal) :: _) if !l.isInstanceOf[Text] =>
        throw ExecutionException("Prefetch keys must be text.")
      case (PREFETCH :: rest, p :: rem) =>
        reduce(rest, Expression(PREFETCH, p :: Nil) :: rem)

      case (REPEAT :: _, Flag(true) :: _) =>
        throw ExecutionException("Infinite loop.")
      case (REPEAT :: rest, Flag(false) :: b :: rem) =>
        reduce(rest, rem)
      case (REPEAT :: _, (l: Literal) :: _) if !l.isInstanceOf[Flag] =>
        throw ExecutionException("Loop condition must evaluate to boolean.")
      case (REPEAT :: rest, c :: b :: rem) =>
        reduce(rest, Expression(REPEAT, c :: b :: Nil) :: rem)

      case (CONS :: rest, f :: s :: rem) if f.isInstanceOf[Literal] =>
        reduce(Left(s) :: rest, rem)
      case (CONS :: rest, f :: s :: rem) =>
        reduce(rest, Expression(CONS, f :: s :: Nil) :: rem)

      case (BRANCH :: rest, Flag(true) :: pass :: _ :: rem) =>
        reduce(Left(pass) :: rest, rem)
      case (BRANCH :: rest, Flag(false) :: _ :: fail :: rem) =>
        reduce(Left(fail) :: rest, rem)
      case (BRANCH :: _, (l: Literal) :: _) if !l.isInstanceOf[Flag] =>
        throw ExecutionException("Branch condition must evaluate to boolean.")
      case (BRANCH :: rest, c :: p :: f :: rem) =>
        reduce(rest, Expression(BRANCH, c :: p :: f :: Nil) :: rem)

      // String/Sequence Operations.
      case (MATCHES :: rest, Text(x) :: Text(regex) :: rem) =>
        reduce(rest, Flag(x.matches(regex)) :: rem)
      case (MATCHES :: _, (l: Literal) :: _) if !l.isInstanceOf[Text] =>
        throw ExecutionException("Match string must evaluate to Text.")
      case (MATCHES :: _, _ :: (l: Literal) :: _) if !l.isInstanceOf[Text] =>
        throw ExecutionException("Match regex must evaluate to Text.")
      case (MATCHES :: rest, x :: r :: rem) =>
        reduce(rest, Expression(MATCHES, x :: r :: Nil) :: rem)

      case (CONTAINS :: rest, Text(x) :: Text(y) :: rem) =>
        reduce(rest, Flag(x.contains(y)) :: rem)
      case (CONTAINS :: _, (l: Literal) :: _) if !l.isInstanceOf[Text] =>
        throw ExecutionException("Contains must be called on text.")
      case (CONTAINS :: _, _ :: (l: Literal) :: _) if !l.isInstanceOf[Text] =>
        throw ExecutionException("Contains must be called on text.")
      case (CONTAINS :: rest, x :: y :: rem) =>
        reduce(rest, Expression(CONTAINS, x :: y :: Nil) :: rem)

      case (LENGTH :: rest, Text(x) :: rem) =>
        reduce(rest, Real(x.length) :: rem)
      case (LENGTH :: _, (l: Literal) :: _) if !l.isInstanceOf[Text] =>
        throw ExecutionException("Length must be called on text.")
      case (LENGTH :: rest, x :: rem) =>
        reduce(rest, Expression(LENGTH, x :: Nil) :: rem)

      case (CONCAT :: rest, Text(x) :: Text(y) :: rem) =>
        reduce(rest, Text(x + y) :: rem)
      case (CONCAT :: rest, Text(x) :: Real(y) :: rem) =>
        reduce(rest, Text(x + y.toString) :: rem)
      case (CONCAT :: rest, Real(x) :: Text(y) :: rem) =>
        reduce(rest, Text(x.toString + y) :: rem)
      case (CONCAT :: rest, Real(x) :: Real(y) :: rem) =>
        reduce(rest, Text(x.toString + y.toString) :: rem)
      case (CONCAT :: rest, Text(x) :: Flag(y) :: rem) =>
        reduce(rest, Text(x + y.toString) :: rem)
      case (CONCAT :: rest, Flag(x) :: Text(y) :: rem) =>
        reduce(rest, Text(x.toString + y) :: rem)
      case (CONCAT :: rest, Flag(x) :: Real(y) :: rem) =>
        reduce(rest, Text(x.toString + y.toString) :: rem)
      case (CONCAT :: rest, Real(x) :: Flag(y) :: rem) =>
        reduce(rest, Text(x.toString + y.toString) :: rem)
      case (CONCAT :: rest, Flag(x) :: Flag(y) :: rem) =>
        reduce(rest, Text(x.toString + y.toString) :: rem)
      case (CONCAT :: rest, x :: y :: rem) =>
        reduce(rest, Expression(CONCAT, x :: y :: Nil) :: rem)

      case (SLICE :: rest, Text(x) :: Real(l) :: Real(h) :: rem) =>
        reduce(rest, Text(x.substring(l.toInt, h.toInt)) :: rem)
      case (SLICE :: _, (l: Literal) :: _) if !l.isInstanceOf[Text] =>
        throw ExecutionException("Slice must be called on text.")
      case (SLICE :: _, _ :: (l: Literal) :: _) if !l.isInstanceOf[Real] =>
        throw ExecutionException("Slice lower bound must be a number.")
      case (SLICE :: _, _ :: _ :: (l: Literal) :: _) if !l.isInstanceOf[Real] =>
        throw ExecutionException("Slice upper bound must be a number.")
      case (SLICE :: rest, x :: l :: h :: rem) =>
        reduce(rest, Expression(SLICE, x :: l :: h :: Nil) :: rem)

      // Math Expressions.
      case (ADD :: rest, Real(x) :: Real(y) :: rem) =>
        reduce(rest, Real(x + y) :: rem)
      case (ADD :: _, (l: Literal) :: _) if !l.isInstanceOf[Real] =>
        throw ExecutionException("Addend must be a Number.")
      case (ADD :: _, _ :: (l: Literal) :: _) if !l.isInstanceOf[Real] =>
        throw ExecutionException("Addend must be a Number.")
      case (ADD :: rest, x :: y :: rem) =>
        reduce(rest, Expression(ADD, x :: y :: Nil) :: rem)

      case (SUB :: rest, Real(x) :: Real(y) :: rem) =>
        reduce(rest, Real(x - y) :: rem)
      case (SUB :: _, (l: Literal) :: _) if !l.isInstanceOf[Real] =>
        throw ExecutionException("Sub must be called on numbers.")
      case (SUB :: _, _ :: (l: Literal) :: _) if !l.isInstanceOf[Real] =>
        throw ExecutionException("Sub must be called on numbers.")
      case (SUB :: rest, x :: y :: rem) =>
        reduce(rest, Expression(SUB, x :: y :: Nil) :: rem)

      case (MUL :: rest, Real(x) :: Real(y) :: rem) =>
        reduce(rest, Real(x * y) :: rem)
      case (MUL :: _, (l: Literal) :: _) if !l.isInstanceOf[Real] =>
        throw ExecutionException("Mul must be called on numbers.")
      case (MUL :: _, _ :: (l: Literal) :: _) if !l.isInstanceOf[Real] =>
        throw ExecutionException("Mul must be called on numbers.")
      case (MUL :: rest, x :: y :: rem) =>
        reduce(rest, Expression(MUL, x :: y :: Nil) :: rem)

      case (DIV :: rest, Real(x) :: Real(y) :: rem) if y == 0 =>
        throw ExecutionException("Cannot divide by zero.")
      case (DIV :: rest, Real(x) :: Real(y) :: rem) =>
        reduce(rest, Real(x / y) :: rem)
      case (DIV :: _, (l: Literal) :: _) if !l.isInstanceOf[Real] =>
        throw ExecutionException("Dividend must be a Number.")
      case (DIV :: _, _ :: (l: Literal) :: _) if !l.isInstanceOf[Real] =>
        throw ExecutionException("Divisor must be a Number.")
      case (DIV :: rest, x :: y :: rem) =>
        reduce(rest, Expression(DIV, x :: y :: Nil) :: rem)

      case (MOD :: rest, Real(x) :: Real(y) :: rem) =>
        reduce(rest, Real(x % y) :: rem)
      case (MOD :: _, (l: Literal) :: _) if !l.isInstanceOf[Real] =>
        throw ExecutionException("Mod must be called on numbers.")
      case (MOD :: _, _ :: (l: Literal) :: _) if !l.isInstanceOf[Real] =>
        throw ExecutionException("Mod must be called on numbers.")
      case (MOD :: rest, x :: y :: rem) =>
        reduce(rest, Expression(MOD, x :: y :: Nil) :: rem)

      case (POW :: rest, Real(x) :: Real(y) :: rem) =>
        reduce(rest, Real(math.pow(x, y)) :: rem)
      case (POW :: _, (l: Literal) :: _) if !l.isInstanceOf[Real] =>
        throw ExecutionException("Pow must be called on numbers.")
      case (POW :: _, _ :: (l: Literal) :: _) if !l.isInstanceOf[Real] =>
        throw ExecutionException("Pow must be called on numbers.")
      case (POW :: rest, x :: y :: rem) =>
        reduce(rest, Expression(POW, x :: y :: Nil) :: rem)

      case (LOG :: rest, Real(x) :: rem) =>
        reduce(rest, Real(math.log(x)) :: rem)
      case (LOG :: _, (l: Literal) :: _) if !l.isInstanceOf[Real] =>
        throw ExecutionException("Log must be called on a number.")
      case (LOG :: rest, x :: rem) =>
        reduce(rest, Expression(LOG, x :: Nil) :: rem)

      case (SIN :: rest, Real(x) :: rem) =>
        reduce(rest, Real(math.sin(x)) :: rem)
      case (SIN :: _, (l: Literal) :: _) if !l.isInstanceOf[Real] =>
        throw ExecutionException("Sin must be called on a number.")
      case (SIN :: rest, x :: rem) =>
        reduce(rest, Expression(SIN, x :: Nil) :: rem)

      case (COS :: rest, Real(x) :: rem) =>
        reduce(rest, Real(math.cos(x)) :: rem)
      case (COS :: _, (l: Literal) :: _) if !l.isInstanceOf[Real] =>
        throw ExecutionException("Cos must be called on a number.")
      case (COS :: rest, x :: rem) =>
        reduce(rest, Expression(COS, x :: Nil) :: rem)

      case (FLOOR :: rest, Real(x) :: rem) =>
        reduce(rest, Real(math.floor(x)) :: rem)
      case (FLOOR :: _, (l: Literal) :: _) if !l.isInstanceOf[Real] =>
        throw ExecutionException("Floor must be called on a number.")
      case (FLOOR :: rest, x :: rem) =>
        reduce(rest, Expression(FLOOR, x :: Nil) :: rem)

      // Logical Expressions.
      case (AND :: rest, Flag(x) :: Flag(y) :: rem) =>
        reduce(rest, Flag(x && y) :: rem)
      case (AND :: _, (l: Literal) :: _) if !l.isInstanceOf[Flag] =>
        throw ExecutionException("And must be called on booleans.")
      case (AND :: _, _ :: (l: Literal) :: _) if !l.isInstanceOf[Flag] =>
        throw ExecutionException("And must be called on booleans.")
      case (AND :: rest, x :: y :: rem) =>
        reduce(rest, Expression(AND, x :: y :: Nil) :: rem)

      case (NOT :: rest, Flag(x) :: rem) =>
        reduce(rest, Flag(!x) :: rem)
      case (NOT :: _, (l: Literal) :: _) if !l.isInstanceOf[Flag] =>
        throw ExecutionException("Not must be called on a boolean.")
      case (NOT :: rest, x :: rem) =>
        reduce(rest, Expression(NOT, x :: Nil) :: rem)

      case (OR :: rest, Flag(x) :: Flag(y) :: rem) =>
        reduce(rest, Flag(x || y) :: rem)
      case (OR :: _, (l: Literal) :: _) if !l.isInstanceOf[Flag] =>
        throw ExecutionException("Or must be called on booleans.")
      case (OR :: _, _ :: (l: Literal) :: _) if !l.isInstanceOf[Flag] =>
        throw ExecutionException("Or must be called on booleans.")
      case (OR :: rest, x :: y :: rem) =>
        reduce(rest, Expression(OR, x :: y :: Nil) :: rem)

      // Comparison Expressions.
      case (EQUAL :: rest, Flag(x) :: Flag(y) :: rem) =>
        reduce(rest, Flag(x == y) :: rem)
      case (EQUAL :: rest, Real(x) :: Real(y) :: rem) =>
        reduce(rest, Flag(x == y) :: rem)
      case (EQUAL :: rest, Text(x) :: Text(y) :: rem) =>
        reduce(rest, Flag(x == y) :: rem)
      case (EQUAL :: _, (x: Literal) :: (y: Literal) :: _) if x.getClass != y.getClass =>
        throw ExecutionException("Equal must be called on literals of the same type.")
      case (EQUAL :: rest, x :: y :: rem) =>
        reduce(rest, Expression(EQUAL, x :: y :: Nil) :: rem)

      case (LESS :: rest, Flag(x) :: Flag(y) :: rem) =>
        reduce(rest, Flag(x < y) :: rem)
      case (LESS :: rest, Real(x) :: Real(y) :: rem) =>
        reduce(rest, Flag(x < y) :: rem)
      case (LESS :: rest, Text(x) :: Text(y) :: rem) =>
        reduce(rest, Flag(x < y) :: rem)
      case (LESS :: _, (x: Literal) :: (y: Literal) :: _) if x.getClass != y.getClass =>
        throw ExecutionException("Less must be called on literals of the same type.")
      case (LESS :: rest, x :: y :: rem) =>
        reduce(rest, Expression(LESS, x :: y :: Nil) :: rem)

      // Default Error.
      case _ => throw ExecutionException(s"$stack cannot be applied to $results.")
    }

    // Recursively reduce the transaction, and then conditionally persist all changes made by the
    // transaction to the underlying database if and only if the versions of its various
    // dependencies have not changed. Filter out empty first changes to allow local variables.
    val depends = snapshot.mapValues(_.version).toMap
    val changes = buffer.toMap

    evaluate(transaction).transformWith {
      case Success(r) =>
        put(depends, changes).transformWith {
          case Success(_) => Future(r)
          case Failure(e) => Future.failed(WriteException(e.getMessage))
        }
      case Failure(r: RollbackException) =>
        put(depends, Map.empty).transformWith {
          case Success(_) => Future(r.result)
          case Failure(e) => Future.failed(WriteException(e.getMessage))
        }
      case Failure(e) => Future.failed(e)
    }
  }

  override def execute(
    transaction: thrift.Transaction,
    resultHandler: AsyncMethodCallback[thrift.Literal]
  ): Unit = {
    // Execute the scala transaction and convert the result back to thrift.
    execute(transaction).onComplete {
      case Success(result) => resultHandler.onComplete(result)
      case Failure(e: ReadException) => resultHandler.onError(e)
      case Failure(e: WriteException) => resultHandler.onError(e)
      case Failure(e: ExecutionException) => resultHandler.onError(e)
      case Failure(e) => resultHandler.onError(ExecutionException(s"Unknown error $e"))
    }
  }

}

object TransactionalDatabase {

  /**
   * An exception that indicates that execution should terminate and the local change buffer should
   * be discarded. Effectively makes a transaction read-only.
   *
   * @param result Return value.
   */
  case class RollbackException(result: Literal) extends Exception

}