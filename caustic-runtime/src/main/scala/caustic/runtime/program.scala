package caustic.runtime

import caustic.runtime.Runtime.Fault
import java.io._
import java.nio.charset.Charset
import java.util.Base64
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

  // Default character representation.
  val charset: Charset = Charset.forName("UTF-8")

  /**
   * Constructs a literal from the serialized base64 representation.
   *
   * @param base64 Serialized representation.
   * @return Literal.
   */
  def apply(base64: String): Literal = {
    val bytes = new ByteArrayInputStream(Base64.getDecoder.decode(base64))
    val stream = new ObjectInputStream(bytes)
    stream.readObject().asInstanceOf[Literal]
  }

  // Implicit Operations.
  implicit class SerializationOps(x: Literal) {

    def asBase64: String = {
      val bytes = new ByteArrayOutputStream()
      val stream = new ObjectOutputStream(bytes)
      stream.writeObject(x)
      Base64.getEncoder.encodeToString(bytes.toByteArray)
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