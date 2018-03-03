package caustic.runtime.typing

import caustic.runtime.math
import caustic.runtime
import caustic.runtime._

/**
 *
 */
trait Value[+T <: Primitive] {

  /**
   *
   * @return
   */
  def get: Program

}

object Value {

  /**
   *
   * @param x
   * @tparam X
   */
  implicit class Textual[X <: String](x: Value[X]) {
    def ++[Y <: X](y: Value[X]): Value[String] = add(x, y)
    def contains[Y <: X](y: Value[Y]): Value[Boolean] = runtime.contains(x, y)
    def indexOf[Y <: X](y: Value[Y]): Value[Boolean] = runtime.indexOf(x, y)
    def length[Y <: X](): Value[Int] = runtime.length(x)
    def matches[Y <: X](y: Value[String]): Value[Boolean] = runtime.matches(x, y)
    def substring(l: Value[Int], h: Value[Int] = x.length()): Value[String] = runtime.slice(x, l, h)
  }

  /**
   *
   * @param x
   * @tparam X
   */
  implicit class Arithmetic[X <: Double](x: Value[X]) {
    def unary_- : Value[X] = sub(math.Zero, x)
    def +[Y <: X](y: Value[Y])(implicit ev: Y <:< X): Value[X] = add(x, y)
    def +[Y >: X <: String](y: Value[Y]): Value[Y] = add(x, y)
    def -[Y <: X](y: Value[Y])(implicit ev: Y <:< X): Value[X] = sub(x, y)
    def -[Y >: X <: String](y: Value[Y]): Value[Y] = sub(x, y)
    def *[Y <: X](y: Value[Y])(implicit ev: Y <:< X): Value[X] = mul(x, y)
    def *[Y >: X <: String](y: Value[Y]): Value[Y] = mul(x, y)
    def /[Y <: X](y: Value[Y])(implicit ev: Y <:< X): Value[X] = div(x, y)
    def /[Y >: X <: String](y: Value[Y]): Value[Y] = div(x, y)
    def %[Y <: X](y: Value[Y])(implicit ev: Y <:< X): Value[X] = mod(x, y)
    def %[Y >: X <: String](y: Value[Y]): Value[Y] = mod(x, y)
  }

  /**
   *
   * @param x
   * @tparam X
   */
  implicit class Compare[X <: Primitive](x: Value[X]) {
    def <  [Y <: Primitive](y: Value[Y]): Value[Boolean] = less(x, y)
    def ===[Y <: Primitive](y: Value[Y]): Value[Boolean] = equal(x, y)

    def <= [Y <: Primitive](y: Value[Y]): Value[Boolean] = x < y || x === y
    def >  [Y <: Primitive](y: Value[Y]): Value[Boolean] = !(x <= y)
    def >= [Y <: Primitive](y: Value[Y]): Value[Boolean] = !(x < y)
    def <> [Y <: Primitive](y: Value[Y]): Value[Boolean] = !(x === y)
  }

  /**
   *
   * @param x
   */
  implicit class Logical(x: Value[Boolean]) {
    def &&(y: Value[Boolean]): Value[Boolean] = both(x, y)
    def ||(y: Value[Boolean]): Value[Boolean] = either(x, y)
    def unary_! : Value[Boolean] = negate(x)
  }

}