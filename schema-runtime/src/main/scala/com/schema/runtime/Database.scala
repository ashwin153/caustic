package com.schema.runtime

import com.schema.runtime.Database.{ExecutionException, RollbackedException}
import scala.annotation.tailrec
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
        fold(List(Left(txn)), List.empty) match {
          case l: Literal => Future(l.value)
          case o: Operation => reduce(o)
        }
      }

    @tailrec def fold(
      stack: List[Either[Transaction, Operator]],
      operands: List[Transaction]
    ): Transaction = (stack, operands) match {
      case (Nil, Nil) =>
        operands.head
      case (Nil, _) =>
        throw ExecutionException("Illegal transaction")
      case (Left(l: Literal) :: rest, _)=>
        fold(rest, l :: operands)
      case (Left(Operation(Read, Literal(key) :: Nil)) :: rest, _) =>
        // Reads to a key are first attempted on the local changes and then on the local snapshot.
        val (_, value) = changes.getOrElse(key, snapshot(key))
        val (version, _) = snapshot(key)
        depends += key -> version
        fold(rest, value :: operands)
      case (Left(Operation(Write, Literal(key) :: Literal(value) :: Nil)) :: rest, _) =>
        // Writes to a key do not immediately take effect; they are locally buffered.
        val (version, _) = snapshot(key)
        changes += key -> (version + 1, value)
        fold(rest, value :: operands)
      case (Left(Operation(Branch, cmp :: pass :: fail :: Nil)) :: rest, _) =>
        // Only recursively fold the condition to ensure we only process the branch that is taken.
        fold(Left(cmp) :: Right(Branch) :: rest, pass :: fail :: operands)
      case (Left(Operation(Cons, first :: second :: Nil)) :: rest, _) =>
        // Only recursively fold the first argument of cons to ensure it is processed first.
        fold(Left(first) :: Right(Cons) :: rest, second :: operands)
      case (Left(Operation(Repeat, cmp :: body :: Nil)) :: rest, _) =>
        // Do not fold the body of the repeat block.
        fold(Left(cmp) :: Right(Repeat) :: rest, body :: operands)
      case (Left(Operation(Loop, from :: until :: step :: index :: body :: Nil)) :: rest, _) =>
        // Do not fold the body of the loop block.
        fold(Left(index) :: Left(step) :: Left(until) :: Left(from) :: Right(Loop) :: rest, body :: operands)
      case (Left(o: Operation) :: rest, _) =>
        // Otherwise, recusively fold all the operands of any operation.
        fold(o.operands.reverse.map(Left.apply) ::: Right(o.operator) :: rest, operands)

      // Core Operations.
      case (Right(Read) :: rest, k :: rem) =>
        fold(rest, read(k) :: rem)
      case (Right(Write) :: rest, k :: v :: rem) =>
        fold(rest, write(k, v) :: rem)
      case (Right(Load) :: rest, Literal(k) :: rem) =>
        fold(rest, locals.getOrElse(k, "") :: rem)
      case (Right(Load) :: rest, k :: rem) =>
        fold(rest, load(k) :: rem)
      case (Right(Store) :: rest, Literal(k) :: Literal(v) :: rem) =>
        locals.put(k, v)
        fold(rest, v :: rem)
      case (Right(Store) :: rest, k :: v :: rem) =>
        fold(rest, store(k, v) :: rem)
      case (Right(Rollback) :: _, Literal(m) :: _) =>
        throw RollbackedException(m)
      case (Right(Rollback) :: rest, m :: rem) =>
        fold(rest, rollback(m) :: rem)
      case (Right(Repeat) :: rest, Literal(c) :: b :: rem) =>
        fold(rest, branch(c, cons(b, repeat(c, b)), Literal.Empty) :: rem)
      case (Right(Repeat) :: rest, c :: b :: rem) =>
        fold(rest, repeat(c, b) :: rem)
      case (Right(Loop) :: rest, Literal(f) :: Literal(u) :: Literal(s) :: i :: b :: rem) =>
        val unroll = (f.toDouble.toInt to u.toDouble.toInt by s.toDouble.toInt)
          .map(j => cons(store(i, j), b))
          .reduceLeftOption((a, b) => cons(a, b))
          .getOrElse(Literal.Empty)
        fold(rest, unroll :: rem)
      case (Right(Loop) :: rest, f :: u :: s :: i :: b :: rem) =>
        fold(rest, loop(f, u, s, i, b) :: rem)
      case (Right(Cons) :: rest, Literal(f) :: s :: rem) =>
        fold(Left(s) :: rest, rem)
      case (Right(Cons) :: rest, f :: s :: rem) =>
        fold(rest, cons(f, s) :: rem)

      // Numeric Operations.
      case (Right(Add) :: rest, Literal(x) :: Literal(y) :: rem) =>
        fold(rest, (x.toDouble + y.toDouble) :: rem)
      case (Right(Add) :: rest, x :: y :: rem) =>
        fold(rest, add(x, y) :: rem)
      case (Right(Sub) :: rest, Literal(x) :: Literal(y) :: rem) =>
        fold(rest, (x.toDouble - y.toDouble) :: rem)
      case (Right(Sub) :: rest, x :: y :: rem) =>
        fold(rest, sub(x, y) :: rem)
      case (Right(Mul) :: rest, Literal(x) ::Literal(y) ::  rem) =>
        fold(rest, (x.toDouble * y.toDouble) :: rem)
      case (Right(Mul) :: rest, x :: y :: rem) =>
        fold(rest, mul(x, y) :: rem)
      case (Right(Div) :: rest, Literal(x) :: Literal(y) :: rem) =>
        fold(rest, (x.toDouble / y.toDouble) :: rem)
      case (Right(Div) :: rest, x :: y :: rem) =>
        fold(rest, div(x, y) :: rem)
      case (Right(Mod) :: rest, Literal(x) :: Literal(y) :: rem) =>
        fold(rest, (x.toDouble % y.toDouble) :: rem)
      case (Right(Mod) :: rest, x :: y :: rem) =>
        fold(rest, mod(x, y) :: rem)
      case (Right(Pow) :: rest, Literal(x) :: Literal(y) :: rem) =>
        fold(rest, math.pow(x.toDouble, y.toDouble) :: rem)
      case (Right(Pow) :: rest, x :: y :: rem) =>
        fold(rest, pow(x, y) :: rem)
      case (Right(Log) :: rest, Literal(x) :: rem) =>
        fold(rest, math.log(x.toDouble) :: rem)
      case (Right(Log) :: rest, x :: rem) =>
        fold(rest, log(x) :: rem)
      case (Right(Sin) :: rest, Literal(x) :: rem) =>
        fold(rest, math.sin(x.toDouble) :: rem)
      case (Right(Sin) :: rest, x :: rem) =>
        fold(rest, sin(x) :: rem)
      case (Right(Cos) :: rest, Literal(x) :: rem) =>
        fold(rest, math.cos(x.toDouble) :: rem)
      case (Right(Cos) :: rest, x :: rem) =>
        fold(rest, cos(x) :: rem)
      case (Right(Floor) :: rest, Literal(x) :: rem) =>
        fold(rest, math.floor(x.toDouble) :: rem)
      case (Right(Floor) :: rest, x :: rem) =>
        fold(rest, floor(x) :: rem)

      // String Operations.
      case (Right(Length) :: rest, Literal(x) :: rem) =>
        fold(rest, x.length :: rem)
      case (Right(Length) :: rest, x :: rem) =>
        fold(rest, length(x) :: rem)
      case (Right(Slice) :: rest, Literal(x) :: Literal(l) :: Literal(h) :: rem) =>
        fold(rest, x.substring(l.toDouble.toInt, h.toDouble.toInt) :: rem)
      case (Right(Slice) :: rest, x :: l :: h :: rem) =>
        fold(rest, slice(x, l, h) :: rem)
      case (Right(Concat) :: rest, Literal(x) :: Literal(y) :: rem) =>
        fold(rest, (x + y) :: rem)
      case (Right(Concat) :: rest, x :: y :: rem) =>
        fold(rest, concat(x, y) :: rem)
      case (Right(Contains) :: rest, Literal(x) :: Literal(y) :: rem) =>
        fold(rest, x.contains(y) :: rem)
      case (Right(Contains) :: rest, x :: y :: rem) =>
        fold(rest, contains(x, y) :: rem)
      case (Right(Matches) :: rest, Literal(x) :: Literal(y) :: rem) =>
        fold(rest, x.matches(y) :: rem)
      case (Right(Matches) :: rest, x :: y :: rem) =>
        fold(rest, matches(x, y) :: rem)

      // Logical Operations.
      case (Right(Branch) :: rest, Literal(c) :: p :: f :: rem) =>
        val take =  if (c != Literal.False.value) p else f
        fold(Left(take) :: rest, rem)
      case (Right(Branch) :: rest, c :: p :: f :: rem) =>
        fold(rest, branch(c, p, f) :: rem)
      case (Right(Equal) :: rest, Literal(x) :: Literal(y) :: rem) =>
        val res = if (x == y) Literal.True else Literal.False
        fold(rest, res :: rem)
      case (Right(Equal) :: rest, x :: y :: rem) =>
        fold(rest, equal(x, y) :: rem)
      case (Right(And) :: rest, Literal(x) :: Literal(y) :: rem) =>
        val res = if (x == Literal.True.value && y == Literal.True.value) Literal.True else Literal.False
        fold(rest, res :: rem)
      case (Right(And) :: rest, x :: y :: rem) =>
        fold(rest, and(x, y) :: rem)
      case (Right(Or) :: rest, Literal(x) :: Literal(y) :: rem) =>
        val res = if (x == Literal.True.value || y == Literal.True.value) Literal.True else Literal.False
        fold(rest, res :: rem)
      case (Right(Or) :: rest, x :: y :: rem) =>
        fold(rest, or(x, y) :: rem)
      case (Right(Less) :: rest, Literal(x) :: Literal(y) :: rem) =>
        val res = if (x < y) Literal.True else Literal.False
        fold(rest, res :: rem)
      case (Right(Less) :: rest, x :: y :: rem) =>
        fold(rest, less(x, y) :: rem)
      case (Right(Not) :: rest, Literal(x) :: rem) =>
        val res = if (x == Literal.True.value) Literal.False else Literal.True
        fold(rest, res :: rem)
      case (Right(Not) :: rest, x :: rem) =>
        fold(rest, not(x) :: rem)
    }

    // Recursively reduce the transaction, and then conditionally persist all changes made by the
    // transaction to the underlying database if and only if the versions of its various
    // dependencies have not changed. Filter out empty first changes to allow local variables.
    reduce(txn).transformWith {
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

  /**
   *
   * @param message
   * @param cause
   */
  case class ExecutionException(
    message: String = "",
    cause: Throwable = None.orNull
  ) extends Exception(message, cause)

}
