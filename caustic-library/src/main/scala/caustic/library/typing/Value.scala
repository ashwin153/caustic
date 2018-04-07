package caustic.library.typing

import caustic.runtime
import caustic.runtime._

/**
 * A scalar value.
 */
trait Value[+T <: Primitive] {

  /**
   * Returns the value as a program.
   *
   * @return Program representation.
   */
  def get: Program

}

object Value {

  // Implicit Operations.
  implicit class AdditionOps[X <: String](x: Value[X]) {
    def +[Y <: X](y: Value[Y])(implicit evidence: Y <:< X): Value[X] = add(x, y)
    def +[Y >: X <: Primitive](y: Value[Y]): Value[Y] = add(x, y)
  }

  implicit class ArithmeticOps[X >: Int <: Double](x: Value[X]) {
    def unary_- : Value[X] = sub(0, x)
    def -(y: Value[Int]): Value[X] = sub(x, y)
    def -[Y >: X <: Double](y: Value[Y])(implicit evidence: X <:< Y): Value[Y] = sub(x, y)
    def *(y: Value[Int]): Value[X] = mul(x, y)
    def *[Y >: X <: Double](y: Value[Y])(implicit evidence: X <:< Y): Value[Y] = mul(x, y)
    def /(y: Value[Int]): Value[X] = div(x, y)
    def /[Y >: X <: Double](y: Value[Y])(implicit evidence: X <:< Y): Value[Y] = div(x, y)
    def %(y: Value[Int]): Value[X] = mod(x, y)
    def %[Y >: X <: Double](y: Value[Y])(implicit evidence: X <:< Y): Value[Y] = mod(x, y)
    def toJson: Value[String] = branch(x, x, "null")
  }

  implicit class TextualOps[X >: String <: Primitive](x: Value[X]) {
    def contains(y: Value[String]): Value[Boolean] = runtime.contains(x, y)
    def indexOf(y: Value[String]): Value[Boolean] = runtime.indexOf(x, y)
    def length: Value[Int] = runtime.length(x)
    def matches(y: Value[String]): Value[Boolean] = runtime.matches(x, y)
    def quoted: Value[String] = add("\"", add(x, "\""))
    def substring(l: Value[Int], h: Value[Int] = length): Value[String] = slice(x, l, h)
    def toJson: Value[String] = branch(x, x.quoted, "null")
  }

  implicit class ComparisonOps[X <: Primitive](x: Value[X]) {
    def <[Y <: Primitive](y: Value[Y]): Value[Boolean] = less(x, y)
    def >[Y <: Primitive](y: Value[Y]): Value[Boolean] = !(x <= y)
    def <=[Y <: Primitive](y: Value[Y]): Value[Boolean] = x < y || x === y
    def >=[Y <: Primitive](y: Value[Y]): Value[Boolean] = !(x < y)
    def <>[Y <: Primitive](y: Value[Y]): Value[Boolean] = !(x === y)
    def ===[Y <: Primitive](y: Value[Y]): Value[Boolean] = equal(x, y)
    def max[Y <: Primitive](y: Value[Y]): Value[Boolean] = branch(x < y, y, x)
    def min[Y <: Primitive](y: Value[Y]): Value[Boolean] = branch(x < y, x, y)
  }

  implicit class LogicalOps[X <: Boolean](x: Value[X]) {
    def unary_! : Value[X] = negate(x)
    def &&(y: Value[X]): Value[X] = both(x, y)
    def ||(y: Value[X]): Value[X] = either(x, y)
  }

}