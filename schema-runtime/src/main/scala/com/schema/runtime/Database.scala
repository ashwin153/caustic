package com.schema.runtime

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import Transaction._
import Transaction.Operation._

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
  ): Future[Map[Key, (Long, Value)]]

  /**
   * Asynchronously applies the specified changes if and only if the versions of the specified
   * dependent keys remain their expected value and returns an exception on conflict. The
   * consistency, durability, and availability of the system depends on the implementation of this
   * conditional put operator.
   *
   * @param depends Version dependencies.
   * @param changes Updates.
   * @param ec Implicit execution context.
   * @return Future that completes when successful, or an exception otherwise.
   */
  def put(depends: Map[Key, Long], changes: Map[Key, (Long, Value)])(
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
  final def execute(txn: Transaction)(
    implicit ec: ExecutionContext
  ): Future[Value] = {
    val snapshot = mutable.Map.empty[Key, (Long, Value)].withDefaultValue((Long.MinValue, ""))
    val changes  = mutable.Map.empty[Key, (Long, Value)]
    val depends  = mutable.Map.empty[Key, Long]

    // Replaces all read operations on literal keys with the most up-to-date value associated with
    // the key, and saves all write operations to a literal key and value in the change buffer and
    // replaces them with the updated value.
    def evaluate(txn: Transaction): Transaction = txn match {
      case l: Literal => l
      case o: Operation => o match {
        case Operation(Read, Literal(key) :: Nil) =>
          // Reads to a key are first attempted on the local changes and then on the local
          // snapshot in order to guarantee that all reads return the latest known version for
          // any particular key. If a key was modified within a transaction, the result of
          // read should reflect these modifications.
          val (_, value) = changes.getOrElse(key, snapshot(key))
          val (version, _) = snapshot(key)
          depends += key -> version
          Literal(value)
        case Operation(Write, Literal(key) :: Literal(value) :: Nil) =>
          // Writes to a key do not immediately take effect; instead, they are stored in the
          // local change buffer and are committed at the end of execution. Writes operators
          // return the value that was written to the key.
          val (version, _) = snapshot(key)
          changes += key -> (version + 1, value)
          Literal(value)
        case _ => o.copy(operands = o.operands.map(evaluate))
      }
    }

    // Folds operations on constants into constants. Because all operations must eventually take
    // literals as arguments and all operations are reducible to literals, all transactions must
    // eventually be foldable into literals.
    def fold(txn: Transaction): Transaction = txn match {
      case l: Literal => l
      case o: Operation => o.copy(operands = o.operands.map(fold)) match {
        case Operation(Purge, Literal(list) :: Nil) =>
          // Purge is an incredibly specialized operator that deletes a sequence of list
          // delimited keys and enables deletion of dynamic objects. For example, purging the
          // string "key,foo,bar" would delete key, key$foo, and key$bar. Purge is a dangerous
          // operator and its use should be restricted to the core runtime library.
          list.split(ListDelimiter.value)
            .filter(_.nonEmpty)
            .map(key => write(key, Literal.Empty))
            .reduceLeftOption((a, b) => cons(a, b))
            .getOrElse(Literal.Empty)
        case Operation(Cons, Literal(_) :: Literal(y) :: Nil) =>
          // Cons is the building block for chaining operations together, because it enables
          // operations to be "glued" together into arbitrary length sequences of operations.
          literal(y)
        case Operation(Add, Literal(x) :: Literal(y) :: Nil) =>
          // Returns the floating point sum of the literals interpreted as numbers. May throw
          // a NumberFormatException if the arguments cannot be converted to numbers.
          literal(x.toDouble + y.toDouble)
        case Operation(Sub, Literal(x) :: Literal(y) :: Nil) =>
          // Returns the floating point difference of the literals interpreted as numbers. May
          // throw a NumberFormatException if the arguments cannot be converted to numbers.
          literal(x.toDouble - y.toDouble)
        case Operation(Mul, Literal(x) :: Literal(y) :: Nil) =>
          // Returns the product point difference of the literals interpreted as numbers. May
          // throw a NumberFormatException if the arguments cannot be converted to numbers.
          literal(x.toDouble * y.toDouble)
        case Operation(Div, Literal(x) :: Literal(y) :: Nil) =>
          // Returns the floating point quotient of the literals interpreted as numbers. May
          // throw a NumberFormatException if the arguments cannot be converted to numbers.
          literal(x.toDouble / y.toDouble)
        case Operation(Mod, Literal(x) :: Literal(y) :: Nil) =>
          // Returns the floating point modulo of the literals interpreted as numbers. May throw
          // a NumberFormatException if the arguments cannot be converted to numbers.
          literal(x.toDouble % y.toDouble)
        case Operation(Pow, Literal(x) :: Literal(y) :: Nil) =>
          // Returns the floating point power of the literals interpreted as numbers. May throw
          // a NumberFormatException if the arguments cannot be converted to numbers.
          literal(math.pow(x.toDouble, y.toDouble))
        case Operation(Log, Literal(x) :: Nil) =>
          // Returns the floating point natural log of the literal interpreted as a number. May
          // throw a NumberFormatException if the argument cannot be converted to
          // numbers or return NaN if the logarithm cannot be computed.
          literal(math.log(x.toDouble))
        case Operation(Sin, Literal(x) :: Nil) =>
          // Returns the sin of the literal argument interpreted as a number. May throw a
          // NumberFormatException if the argument cannot be converted to na umber.
          literal(math.sin(x.toDouble))
        case Operation(Cos, Literal(x) :: Nil) =>
          // Returns the cosine of the literal argument interpreted as a number. May throw a
          // NumberFormatException if the argument cannot be converted to a number.
          literal(math.cos(x.toDouble))
        case Operation(Floor, Literal(x) :: Nil) =>
          // Returns the integer floor of the literal argument interpreted as a number. May
          // throw a NumberFormatException if the argument cannot be converted to a number.
          literal(math.floor(x.toDouble))
        case Operation(Length, Literal(x) :: Nil) =>
          // Returns the number of characters in the literal argument.
          literal(x.length)
        case Operation(Slice, Literal(x) :: Literal(l) :: Literal(h) :: Nil) =>
          // Returns the substring of the first argument with the remaining literals interpreted
          // as the lower and upper bounds respectively. Slice may throw a
          // StringIndexOutOfBoundsException if the bounds are invalid.
          literal(x.substring(l.toInt, h.toInt))
        case Operation(Concat, Literal(x) :: Literal(y) :: Nil) =>
          // Returns the concatenation of the literal arguments.
          literal(x + y)
        case Operation(Branch, Literal(cmp) :: pass :: fail :: Nil) =>
          // Returns the third argument if the first literal is nonEmpty, or the second argument
          // otherwise. Note: Literal.Empty == Literal.False.
          if (cmp != Literal.False.value) pass else fail
        case Operation(Equals, Literal(x) :: Literal(y) :: Nil) =>
          // Returns True if the arguments are identical and False otherwise.
          if (x == y) Literal.True else Literal.False
        case Operation(Matches, Literal(x) :: Literal(y) :: Nil) =>
          // Returns True if the first arguments matches the regular expression specified by the
          // second argument and False otherwise.
          if (x.matches(y)) Literal.True else Literal.False
        case Operation(And, Literal(x) :: Literal(y) :: Nil) =>
          // Returns True if both arguments are notEmpty, and False otherwise.
          if (x.nonEmpty && y.nonEmpty) Literal.True else Literal.False
        case Operation(Or, Literal(x) :: Literal(y) :: Nil) =>
          // Returns True if either argument is notEmpty, and False otherwise.
          if (x.nonEmpty || y.nonEmpty) Literal.True else Literal.False
        case Operation(Not, Literal(x) :: Nil) =>
          // Returns False if the argument is nonEmpty, and True otherwise.
          if (x.nonEmpty) Literal.False else Literal.True
        case Operation(Less, Literal(x) :: Literal(y) :: Nil) =>
          // Returns True if the first argument is lexicographically less than the second
          // argument, and False otherwise.
          if (x < y) Literal.True else Literal.False
        case default =>
          // All other transactions cannot be simplified yet.
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
    def reduce(txn: Transaction): Future[String] = {
      get(txn.readset ++ txn.writeset -- snapshot.keys -- depends.keys) flatMap { values =>
        snapshot ++= values
        fold(evaluate(txn)) match {
          case l: Literal => Future(l.value)
          case o: Operation => reduce(o)
        }
      }
    }

    // Recursively reduce the transaction, and then conditionally persist all changes made by the
    // transaction to the underlying database if and only if the versions of its various
    // dependencies have not changed.
    reduce(fold(txn)).flatMap(r => put(depends.toMap, changes.toMap).map(_ => r))
  }

}
