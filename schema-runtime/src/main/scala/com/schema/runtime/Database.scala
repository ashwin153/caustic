package com.schema.runtime

import com.schema.runtime.Database.RollbackedException
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
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
    val snapshot = mutable.Map.empty[Key, (Revision, Value)].withDefaultValue((0L, ""))
    val changes  = mutable.Map.empty[Key, (Revision, Value)]
    val depends  = mutable.Map.empty[Key,  Revision]
    val locals   = mutable.Map.empty[Key,  Value]

    // Replaces all read operations on literal keys with the most up-to-date value associated with
    // the key, and saves all write operations to a literal key and value in the change buffer and
    // replaces them with the updated value.
    def evaluate(txn: Transaction): Transaction = txn match {
      case l: Literal => l
      case o: Operation => o match {
        case Operation(Read, Literal(key) :: Nil) =>
          // Reads to a key are first attempted on the local changes and then on the local snapshot
          // in order to guarantee that all reads return the latest known version for any particular
          // key. If a key was modified within a transaction, the result of read should reflect
          // these modifications.
          val (_, value) = changes.getOrElse(key, snapshot(key))
          val (version, _) = snapshot(key)
          depends += key -> version
          Literal(value)
        case Operation(Write, Literal(key) :: Literal(value) :: Nil) =>
          // Writes to a key do not immediately take effect; instead, they are stored in the local
          // change buffer and are committed at the end of execution. Writes operators return the
          // value that was written to the key.
          val (version, _) = snapshot(key)
          changes += key -> (version + 1, value)
          Literal(value)
        case Operation(Branch, cmp :: pass :: fail :: Nil) =>
          // Branches are only evaluated on their comparison condition to ensure that only the
          // branch that is taken is ever evaluated.
          branch(evaluate(cmp), pass, fail)
        case Operation(Cons, first :: second :: Nil) =>
          // Evaluate the first argument before the other.
          cons(evaluate(first), second)
        case Operation(Repeat, cmp :: body :: Nil) =>
          // Do not evaluate the body of the loop.
          repeat(evaluate(cmp), body)
        case Operation(Loop, from :: until :: step :: index :: body :: Nil) =>
          // Do not evaluate the body of the loop.
          loop(evaluate(from), evaluate(until), evaluate(step), evaluate(index), body)
        case _ =>
          // Otherwise, recursively evaluate the operands of the operation.
          o.copy(operands = o.operands.map(evaluate))
      }
    }

    // Folds operations on constants into constants. Because all operations must eventually take
    // literals as arguments and all operations are reducible to literals, all transactions must
    // eventually be foldable into literals.
    def fold(txn: Transaction): Transaction = txn match {
      case l: Literal => l
      case o: Operation => o.copy(operands = o.operands.map(fold)) match {
        case Operation(Repeat, Literal(c) :: b :: Nil) =>
          branch(c, cons(b, repeat(c, b)), Literal.Empty)
        case Operation(Loop, Literal(f) :: Literal(u) :: Literal(s) :: i :: b :: Nil) =>
          (f.toDouble.toInt to u.toDouble.toInt by s.toDouble.toInt)
            .map(j => cons(store(i, j), b))
            .reduceLeftOption((a, b) => cons(a, b))
            .getOrElse(Literal.Empty)
        case Operation(Cons, Literal(_) :: y :: Nil) =>
          y
        case Operation(Add, Literal(x) :: Literal(y) :: Nil) =>
          literal(x.toDouble + y.toDouble)
        case Operation(Sub, Literal(x) :: Literal(y) :: Nil) =>
          literal(x.toDouble - y.toDouble)
        case Operation(Mul, Literal(x) :: Literal(y) :: Nil) =>
          literal(x.toDouble * y.toDouble)
        case Operation(Div, Literal(x) :: Literal(y) :: Nil) =>
          literal(x.toDouble / y.toDouble)
        case Operation(Mod, Literal(x) :: Literal(y) :: Nil) =>
          literal(x.toDouble % y.toDouble)
        case Operation(Pow, Literal(x) :: Literal(y) :: Nil) =>
          literal(math.pow(x.toDouble, y.toDouble))
        case Operation(Log, Literal(x) :: Nil) =>
          literal(math.log(x.toDouble))
        case Operation(Sin, Literal(x) :: Nil) =>
          literal(math.sin(x.toDouble))
        case Operation(Cos, Literal(x) :: Nil) =>
          literal(math.cos(x.toDouble))
        case Operation(Floor, Literal(x) :: Nil) =>
          literal(math.floor(x.toDouble))
        case Operation(Length, Literal(x) :: Nil) =>
          literal(x.length)
        case Operation(Slice, Literal(x) :: Literal(l) :: Literal(h) :: Nil) =>
          literal(x.substring(l.toDouble.toInt, h.toDouble.toInt))
        case Operation(Concat, Literal(x) :: Literal(y) :: Nil) =>
          literal(x + y)
        case Operation(Branch, Literal(cmp) :: pass :: fail :: Nil) =>
          if (cmp != Literal.False.value) pass else fail
        case Operation(Equal, Literal(x) :: Literal(y) :: Nil) =>
          if (x == y) Literal.True else Literal.False
        case Operation(Contains, Literal(x) :: Literal(y) :: Nil) =>
          if (x.contains(y)) Literal.True else Literal.False
        case Operation(Matches, Literal(x) :: Literal(y) :: Nil) =>
          if (x.matches(y)) Literal.True else Literal.False
        case Operation(And, Literal(x) :: Literal(y) :: Nil) =>
          if (x == Literal.True.value && y == Literal.True.value) Literal.True else Literal.False
        case Operation(Or, Literal(x) :: Literal(y) :: Nil) =>
          if (x == Literal.True.value || y == Literal.True.value) Literal.True else Literal.False
        case Operation(Not, Literal(x) :: Nil) =>
          if (x == Literal.True.value) Literal.False else Literal.True
        case Operation(Less, Literal(x) :: Literal(y) :: Nil) =>
          if (x < y) Literal.True else Literal.False
        case Operation(Load, Literal(key) :: Nil) =>
          Literal(locals.getOrElse(key, ""))
        case Operation(Store, Literal(key) :: Literal(value) :: Nil) =>
          locals.put(key, value)
          Literal(value)
        case Operation(Rollback, Literal(msg) :: Nil) =>
          throw RollbackedException(msg)
        case default =>
          default
      }
    }

    // Reduces the transaction to a literal by repeatedly evaluating and folding it. Because all
    // transactions must terminate in literals (they are the "leaves" of the transaction) and all
    // operations are reducible to literals, all transaction must eventually reduce to a literal.
    // Each iteration first loads all keys in the transaction's readset (for their value) and in the
    // transaction's writeset (for their version) that have not been read before (to avoid changes
    // in value) and that are not currently depended on by the transaction (to avoid changes in
    // version) to ensure that the evaluation of the transaction is correct and consistent.
    def reduce(txn: Transaction): Future[String] =
      get(txn.readset ++ txn.writeset -- snapshot.keys -- depends.keys) flatMap { values =>
        snapshot ++= values
        fold(evaluate(txn)) match {
          case l: Literal => Future(l.value)
          case o: Operation => reduce(o)
        }
      }

    // Recursively reduce the transaction, and then conditionally persist all changes made by the
    // transaction to the underlying database if and only if the versions of its various
    // dependencies have not changed. Filter out empty first changes to allow local variables.
    reduce(fold(txn)).transformWith {
      case Success(r) => put(depends.toMap, changes.toMap).map(_ => r)
      case Failure(e: RollbackedException) => Future(e.message)
      case Failure(e) => Future.failed(e)
    }
  }

}

object Database {

  /**
   *
   * @param message
   * @param cause
   */
  case class RollbackedException(
    message: String = "",
    cause: Throwable = None.orNull
  ) extends Exception(message, cause)

}
