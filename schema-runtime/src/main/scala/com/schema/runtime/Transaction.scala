package com.schema.runtime

import com.schema.runtime.Operation._

/**
 * An immutable, database transaction. Transactions form an implicit abstract syntax tree that may
 * be executed by a [[Database]]. Each "node" in this tree is an [[Operation]] and each "leaf" is a
 * [[Literal]].
 */
sealed trait Transaction {

  /**
   * Returns the set of all keys that are read by the transaction.
   *
   * @return Read set.
   */
  def readset: Set[Key]

  /**
   * Returns the set of all keys that may be modified by the transaction.
   *
   * @return Write set.
   */
  def writeset: Set[Key]

}

/**
 * A literal value. Literals are the only way to specify values within a transaction. Therefore,
 * literals form the "leaves" of the implicit expression tree formed by a transaction.
 * Construction of literals is limited to the runtime library to ensure that certain conventions
 * concerning the representation of types (booleans, integers, etc.) are maintained.
 *
 * @param value Literal value.
 */
final case class Literal private[runtime](
  value: Value
) extends Transaction {

  override def readset: Set[Key] = Set.empty

  override def writeset: Set[Key] = Set.empty

  override def toString: String = "\"" + value + "\""

}

object Literal {

  val Empty = Literal("")
  val Zero  = Literal("0.0")
  val Half  = Literal("0.5")
  val One   = Literal("1.0")
  val Two   = Literal("2.0")
  val True  = Literal.One
  val False = Literal.Zero

}

/**
 * A transactional operation. Operations specify functions that are performed on either Literal
 * arguments or the output of other operations. Operations form the "nodes" of the implicit
 * expression tree formed by a transaction. Construction of operations is limited to the runtime
 * library to ensure that all operators are constructed with the correct arity. An alternative
 * implementation might choose to flatten and to make the various operators as direct descendants
 * of transaction. However, doing so would vastly complicate the database execution logic, because
 * it would require separate default cases for all the various operators. This approach is much
 * simpler to implement, and we still recover static type safety by forcing operation construction
 * to happen through specialized type-safe constructors.
 *
 * @param operator Operator.
 * @param operands Operand transactions.
 */
final case class Operation private[runtime](
  operator: Operator,
  operands: List[Transaction]
) extends Transaction {

  override def readset: Set[Key] = this match {
    case Operation(Read, Literal(key) :: Nil) => Set(key)
    case _ => this.operands.foldLeft(Set.empty[Key])(_ ++ _.readset)
  }

  override def writeset: Set[Key] = this match {
    case Operation(Write, Literal(key) :: _ :: Nil) => Set(key)
    case _ => this.operands.foldLeft(Set.empty[Key])(_ ++ _.writeset)
  }

  override def toString: String =
    this.operator.toString + "(" + this.operands.map(_.toString).mkString(", ") + ")"

}

object Operation {

  sealed trait Operator
  case object Read     extends Operator  // Lookup the version and value of a remote key.
  case object Write    extends Operator  // Update the version and value of a remote key.
  case object Load     extends Operator  // Lookup the value of a local key.
  case object Store    extends Operator  // Update the value of a local key.
  case object Cons     extends Operator  // Sequentially evaluate arguments.
  case object Prefetch extends Operator  // Prefetches the comma delimited list of keys.
  case object Repeat   extends Operator  // Repeat while the condition is not satisfied.
  case object Branch   extends Operator  // Jump to third if first is empty, and second otherwise.
  case object Rollback extends Operator  // Rolls back the transaction.
  case object Add      extends Operator  // Sum of the two arguments.
  case object Sub      extends Operator  // Difference of the two arguments.
  case object Mul      extends Operator  // Product of the two arguments.
  case object Div      extends Operator  // Quotient of the two arguments.
  case object Mod      extends Operator  // Modulo of the two arguments.
  case object Pow      extends Operator  // Power of the first argument to the second.
  case object Log      extends Operator  // Natural logarithm of the argument.
  case object Sin      extends Operator  // Sine of the argument.
  case object Cos      extends Operator  // Cosine of the argument.
  case object Floor    extends Operator  // Largest integer less than the argument.
  case object And      extends Operator  // Checks that both arguments are non-empty.
  case object Not      extends Operator  // Opposite of the argument.
  case object Or       extends Operator  // Checks that either argument is non-empty.
  case object Length   extends Operator  // Number of characters in the argument.
  case object Slice    extends Operator  // Substring of the first argument bounded by  others.
  case object Concat   extends Operator  // Concatenation of the two arguments.
  case object Matches  extends Operator  // Regular expression of second argument matches first.
  case object Contains extends Operator  // Checks that the first argument contains the second.
  case object Equal    extends Operator  // Checks that the two arguments are equal.
  case object Less     extends Operator  // Checks that the first argument is less than the other.

}