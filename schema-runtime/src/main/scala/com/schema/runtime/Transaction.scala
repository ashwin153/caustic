package com.schema.runtime

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