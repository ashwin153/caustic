package caustic

import runtime.thrift.Operator._

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.language.implicitConversions

/**
 * Until Scrooge updates its Thrift version in issue #85, it will be incompatible with the current
 * Thrift IDLs. Instead, we'll rely on the Apache Thrift code generator to generate Java files and
 * manually convert them to use immutable Scala collections and case classes.
 */
package object runtime {

  // We may assume without loss of generality that all keys are strings, because all digital
  // information must be encodable as a binary string of ones and zeroes. Each key is associated
  // with a version number, which databases use to detect transaction conflicts during execution.
  type Key = String
  type Version = Long
  type Operator = thrift.Operator
  val Operator = thrift.Operator

  case class ExecutionException(message: String) extends thrift.ExecutionException(message)
  case class WriteException(message: String) extends thrift.WriteException(message)
  case class ReadException(message: String) extends thrift.ReadException(message)

  /**
   * A versioned value. Revisions of a key are totally ordered by their associated version.
   * Revisions are the mechanism through which transactional consistency is achieved; if a newer
   * revision exists for a key that is read or written, then a transaction is rejected.
   */
  @SerialVersionUID(1L)
  case class Revision(version: Version, value: Literal) extends Serializable

  /**
   * A database transaction. Transactions form an implicit abstract syntax tree, in which the nodes
   * are expressions and the leaves are literals. Transactions may be executed by a database.
   */
  sealed trait Transaction {

    /**
     * Returns the set of all keys that are read by the transaction. Each time a transaction is
     * partially evaluated its readset may change. Transactions depend on all of its readsets
     * throughout its execution.
     *
     * @return Read set.
     */
    def readset: Set[Key] = {
      @tailrec
      def fold(stack: List[Transaction], rset: Set[Key]): Set[Key] = stack match {
        case Nil => rset
        case Expression(READ, Text(key) :: Nil) :: rest => fold(rest, rset + key)
        case Expression(_, operands) :: rest => fold(operands ::: rest, rset)
        case _ :: rest => fold(rest, rset)
      }

      fold(List(this), Set.empty)
    }

    /**
     * Returns the set of all keys that may be modified by the transaction. Each time a transaction
     * is partially evaluated its writeset may change. Transactions depend on all of its writesets
     * throughout its execution.
     *
     * @return Write set.
     */
    def writeset: Set[Key] = {
      @tailrec
      def fold(stack: List[Transaction], wset: Set[Key]): Set[Key] = stack match {
        case Nil => wset
        case Expression(WRITE, Text(key) :: _ :: Nil) :: rest => fold(rest, wset + key)
        case Expression(_, operands) :: rest=> fold(operands ::: rest, wset)
        case _ :: rest => fold(rest, wset)
      }

      fold(List(this), Set.empty)
    }

  }

  implicit def ttxn2stxn(txn: thrift.Transaction): Transaction = {
    @tailrec
    def convert(stack: List[Any], operands: List[Transaction]): Transaction = stack match {
      case Nil =>
        operands.head
      case (txn: thrift.Transaction) :: rest if txn.isSetLiteral =>
        convert(rest, txn.getLiteral :: operands)
      case (txn: thrift.Transaction) :: rest if txn.isSetExpression =>
        val expr = txn.getExpression
        convert(expr.operands.asScala.reverse :: expr.operator :: rest, operands)
      case (op: thrift.Operator) :: rest =>
        val arity = (op.getValue >> 8) & 0xFFF
        if (operands.length < arity)
          throw new thrift.ExecutionException(s"Not enough operands for $op")
        else
          convert(rest, Expression(op, operands.take(arity)) :: operands.drop(arity))
    }

    convert(List(txn), List.empty)
  }

  /**
   * A literal value. Literals are the only way to specify explicit values within a transaction.
   * Therefore, literals form the "leaves" of the abstract syntax tree in a transaction. Literals
   * may be pattern matched and
   */
  sealed trait Literal extends Transaction
  case class Flag(value: Boolean) extends Literal
  case class Real(value: Double) extends Literal
  case class Text(value: String) extends Literal

  implicit def slit2tlit(literal: Literal): thrift.Literal = literal match {
    case Flag(value) => thrift.Literal.flag(value)
    case Real(value) => thrift.Literal.real(value)
    case Text(value) => thrift.Literal.text(value)
  }

  implicit def tlit2slit(literal: thrift.Literal): Literal = literal match {
    case l if l.isSetFlag => Flag(l.getFlag)
    case l if l.isSetReal => Real(l.getReal)
    case l if l.isSetText => Text(l.getText)
  }

  /**
   * A transactional operation. Expressions apply Operators to operands that are either Literals or
   * the output of other Expressions. Expressions form the nodes of the abstract syntax tree in a
   * Transaction.
   *
   * @param operator Thrift operator.
   * @param operands List of transaction operands.
   */
  case class Expression(
    operator: Operator,
    operands: List[Transaction]
  ) extends Transaction

}

