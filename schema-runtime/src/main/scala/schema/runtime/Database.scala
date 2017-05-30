package schema.runtime

import Database._
import Operation._
import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.ControlThrowable
import scala.util.{Failure, Success}

/**
 * An asynchronous, key-value store.
 */
trait Database {

  /**
   * Asynchronously returns the versions and values of the specified keys.
   *
   * @param keys Keys to lookup.
   * @param ec Implicit execution context.
   * @return Version and values of specified keys.
   */
  def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, (Revision, Value)]]

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
  def put(depends: Map[Key, Revision], changes: Map[Key, (Revision, Value)])(
    implicit ec: ExecutionContext
  ): Future[Unit]

  /**
   * Asynchronously executes the specified transaction and returns the result. Transactions are
   * repeatedly partially evaluated until they are reduced to a literal value. Keys are read from
   * the underlying database and are written to a local change buffer that is conditionally put
   * in the underlying database if and only if the read dependencies of the transaction remain
   * unchanged.
   *
   * @param txn Transaction to execute.
   * @param ec Implicit execution context.
   * @return Result of transaction execution, or an exception on failure.
   */
  def execute(txn: Transaction)(
    implicit ec: ExecutionContext
  ): Future[Value] = {
    val snapshot = mutable.Map.empty[Key, (Revision, Value)]
    val changes  = mutable.Map.empty[Key, (Revision, Value)]
    val depends  = mutable.Map.empty[Key,  Revision]
    val locals   = mutable.Map.empty[Key,  Value]

    // Reduces the transaction to a literal by repeatedly evaluating and folding it. Because all
    // transactions must terminate in literals (they are the "leaves" of the transaction) and all
    // operations are reducible to literals, all transaction must eventually reduce to a literal.
    // Each iteration first loads all keys in the transaction's readset (for their value) and in the
    // transaction's writeset (for their version) that have not been read before (to avoid changes
    // in value) and that are not currently depended on by the transaction (to avoid changes in
    // version) to ensure that the evaluation of the transaction is correct and consistent.
    def reduce(txn: Transaction): Future[String] = {
      val keys = txn.readset ++ txn.writeset -- snapshot.keys
      snapshot ++= keys.map(k => k -> (0L, ""))

      Future(keys)
        .flatMap(k => if (k.nonEmpty) get(k).map(snapshot ++= _) else Future.unit)
        .flatMap(_ => fold(List(Left(txn)), List.empty) match {
          case l: Literal => Future(l.value)
          case o: Operation => reduce(o)
        })
    }

    @tailrec def fold(
      stack: List[Either[Transaction, Operator]],
      operands: List[Transaction]
    ): Transaction = stack match {
      case Nil =>
        if (operands.size > 1)
          throw ParseException(s"Transaction evaluates to $operands.")
        operands.head
      case Left(l: Literal) :: rest =>
        fold(rest, l :: operands)
      case Left(o: Operation) :: rest =>
        o match {
          case Operation(Read, Literal(key) :: Nil) =>
            // Replace all reads to known keys with their latest value.
            val (_, value) = changes.getOrElse(key, snapshot(key))
            val (version, _) = snapshot(key)
            depends += key -> version
            fold(rest, literal(value) :: operands)
          case Operation(Write, Literal(key) :: Literal(value) :: Nil) =>
            // Save all writes to the local change buffer.
            val (version, _) = snapshot(key)
            changes += key -> (version + 1, value)
            fold(rest, literal(value) :: operands)
          case Operation(Branch, cmp :: pass :: fail :: Nil) =>
            // Do not evaluate the branch that is not taken.
            fold(Left(cmp) :: Right(Branch) :: rest, pass :: fail :: operands)
          case Operation(Cons, first :: second :: Nil) =>
            // Do not evaluate the second argument before the first.
            fold(Left(first) :: Right(Cons) :: rest, second :: operands)
          case Operation(Repeat, cond :: body :: Nil) =>
            // Do not evaluate the body of a loop before evaluating the condition.
            fold(Left(branch(cond, cons(body, repeat(cond, body)), Literal.Empty)) :: rest, operands)
          case _ =>
            // Otherwise, recurse on all opernds.
            fold(o.operands.reverse.map(Left.apply) ::: Right(o.operator) :: rest, operands)
        }
      case Right(op) :: rest =>
        (op, operands) match {
          // Core Operations.
          case (Read, k :: rem) => fold(rest, read(k) :: rem)
          case (Write, k :: v :: rem) => fold(rest, write(k, v) :: rem)
          case (Load, Literal(k) :: rem) => fold(rest, literal(locals.getOrElse(k, "")) :: rem)
          case (Load, k :: rem) => fold(rest, load(k) :: rem)
          case (Store, Literal(k) :: Literal(v) :: rem) => locals.put(k, v); fold(rest, literal(v) :: rem)
          case (Store, k :: v :: rem) => fold(rest, store(k, v) :: rem)
          case (Rollback, Literal(m) :: _) => throw RollbackException(m)
          case (Rollback, m :: rem) => fold(rest, rollback(m) :: rem)
          case (Repeat, c :: b :: rem) => fold(rest, repeat(c, b) :: rem)
          case (Prefetch, l :: rem) => fold(rest, prefetch(l) :: rem)
          case (Cons, Literal(_) :: s :: rem) => fold(Left(s) :: rest, rem)
          case (Cons, f :: s :: rem) => fold(rest, cons(f, s) :: rem)
          case (Branch, Literal(c) :: p :: f :: rem) => fold(Left(branch(literal(c), p, f)) :: rest, rem)
          case (Branch, c :: p :: f :: rem) => fold(rest, branch(c, p, f) :: rem)

          // Numeric Operations.
          case (Add, x :: y :: rem) => fold(rest, add(x, y) :: rem)
          case (Sub, x :: y :: rem) => fold(rest, sub(x, y) :: rem)
          case (Mul, x :: y :: rem) => fold(rest, mul(x, y) :: rem)
          case (Div, x :: y :: rem) => fold(rest, div(x, y) :: rem)
          case (Mod, x :: y :: rem) => fold(rest, mod(x, y) :: rem)
          case (Pow, x :: y :: rem) => fold(rest, pow(x, y) :: rem)
          case (Log, x :: rem) => fold(rest, log(x) :: rem)
          case (Sin, x :: rem) => fold(rest, sin(x) :: rem)
          case (Cos, x :: rem) => fold(rest, cos(x) :: rem)
          case (Floor, x :: rem) => fold(rest, floor(x) :: rem)

          // String Operations.
          case (Length, x :: rem) => fold(rest, length(x) :: rem)
          case (Slice, x :: l :: h :: rem) => fold(rest, slice(x, l, h) :: rem)
          case (Concat, x :: y :: rem) => fold(rest, concat(x, y) :: rem)
          case (Contains, x :: y :: rem) => fold(rest, contains(x, y) :: rem)
          case (Matches, x :: y :: rem) => fold(rest, matches(x, y) :: rem)

          // Logical Operations.
          case (Equal, x :: y :: rem) => fold(rest, equal(x, y) :: rem)
          case (And, x :: y :: rem) => fold(rest, and(x, y) :: rem)
          case (Or, x :: y :: rem) => fold(rest, or(x, y) :: rem)
          case (Less, x :: y :: rem) => fold(rest, less(x, y) :: rem)
          case (Not, x :: rem) => fold(rest, not(x) :: rem)
          case _ => throw ParseException(s"$op cannot be applied to $operands.")
        }
    }

    // Recursively reduce the transaction, and then conditionally persist all changes made by the
    // transaction to the underlying database if and only if the versions of its various
    // dependencies have not changed. Filter out empty first changes to allow local variables.
    reduce(txn).transformWith {
      case Success(r) => put(depends.toMap, changes.toMap).map(_ => r)
      case Failure(e: RollbackException) => put(depends.toMap, Map.empty).map(_ => e.message)
      case Failure(e) => Future.failed(e)
    }
  }

}

object Database {

  /**
   * A failure indicating that any changes performed by the transaction should be discarded and
   * that the specified message should be returned in their place.
   *
   * @param message Rollback message.
   */
  case class RollbackException(
    message: String
  ) extends Exception(message)

  /**
   * A failure indicating that a transaction cannot be parsed and should be aborted. Parse
   * exceptions cannot be retried, and therefore inherit from [[ControlThrowable]].
   *
   * @param message Exception message.
   * @param cause Exception cause.
   */
  case class ParseException(
    message: String = "",
    cause: Throwable = None.orNull
  ) extends Exception(message, cause) with ControlThrowable

}
