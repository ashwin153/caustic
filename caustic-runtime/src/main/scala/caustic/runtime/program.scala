package caustic.runtime

import caustic.runtime.Runtime.Fault

import java.io._
import scala.util.Try

/**
 * A procedure. Programs are abstract-syntax trees composed of literals and expressions.
 */
sealed trait Program

/**
 * A literal transformation. Expressions map literal operands to a literal result using their
 * operator. Because all expressions return literal values on literal arguments, every program can
 * eventually be reduced to a single literal value.
 *
 * @param operator Transformation.
 * @param operands Arguments.
 */
case class Expression(operator: Operator, operands: List[Program]) extends Program

/**
 * A typed value. Literals contain primitive values of type null, flag, real, or text which
 * correspond to null, boolean, double, and string respectively.
 */
sealed trait Literal extends Program with Serializable
case object Null extends Literal
case class Flag(value: Boolean) extends Literal
case class Real(value: Double) extends Literal
case class Text(value: String) extends Literal

object Literal {

  /**
   * Constructs a literal from the serialized binary representation.
   *
   * @param binary Serialized representation.
   * @return Literal.
   */
  def apply(binary: String): Literal = binary match {
    case x if x.isEmpty => Null
    case x if x(0) == 'f' => if (x(1) == '1') flag(true) else flag(false)
    case x if x(0) == 'r' => Real(x.substring(1).toDouble)
    case x if x(0) == 't' => Text(x.substring(1))
  }

  // Implicit Operations.
  implicit class SerializationOps(x: Literal) {

    def asBinary: String = x match {
      case Null => ""
      case Flag(t) => if (t) "f1" else "f0"
      case Real(t) => "r" + t
      case Text(t) => "t" + t
    }

    def asString: String = x match {
      case Null => "null"
      case Flag(a) => a.toString
      case Real(a) => if (a == math.floor(a)) a.toInt.toString else a.toString
      case Text(a) => a
    }

    def asDouble: Double = x match {
      case Null => 0
      case Flag(a) => if (a) 1 else 0
      case Real(a) => a
      case Text(a) => Try(a.toDouble).getOrElse(throw Fault(s"Unable to convert $a to double"))
    }

    def asInt: Int = x match {
      case Null => 0
      case Flag(a) => if (a) 1 else 0
      case Real(a) => a.toInt
      case Text(a) => Try(a.toInt).getOrElse(throw Fault(s"Unable to convert $a to int"))
    }

    def asBoolean: Boolean = x match {
      case Null => false
      case Flag(a) => a
      case Real(a) => a != 0
      case Text(a) => a.nonEmpty
    }

  }

}