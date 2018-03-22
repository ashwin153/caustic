package caustic.runtime

import java.io._
import java.nio.charset.Charset
import java.util.Base64

/**
 * A transactional procedure. Programs are an abstract-syntax tree composed of [[Literal]] leaves
 * and [[Expression]] nodes.
 */
sealed trait Program

/**
 * A [[Literal]] transformation. Expressions map [[Literal]] operands to a [[Literal]] result
 * using their [[Operator]]. Because all expressions produce [[Literal]] results on [[Literal]]
 * arguments, every [[Program]] can eventually be reduced to a single [[Literal]].
 *
 * @param operator
 * @param operands
 */
case class Expression(operator: Operator, operands: List[Program]) extends Program

/**
 * A typed value. Literals contain primitive values of type [[Null]], [[Flag]], [[Real]], and
 * [[Text]] which correspond to null, boolean, double, and string respectively.
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
   *
   * @param repr
   * @return
   */
  def deserialize(repr: String): Literal = {
    val bytes  = new ByteArrayInputStream(Base64.getDecoder.decode(repr))
    val stream = new ObjectInputStream(bytes)
    stream.readObject().asInstanceOf[Literal]
  }

  /**
   *
   * @param literal
   * @return
   */
  def serialize(literal: Literal): String = {
    val bytes  = new ByteArrayOutputStream()
    val stream = new ObjectOutputStream(bytes)
    stream.writeObject(literal)
    Base64.getEncoder.encodeToString(bytes.toByteArray)
  }

}