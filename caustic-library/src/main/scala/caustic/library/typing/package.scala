package caustic.library

import caustic.library.control.Context
import caustic.runtime
import caustic.runtime._

import scala.language.implicitConversions

package object typing {

  val Null                                          : Program           = runtime.Null
  val True                                          : Value[Boolean]    = Constant(true)
  val False                                         : Value[Boolean]    = Constant(false)

  // Implicit Conversions.
  implicit def program(x: Value[_])                 : Program           = x.get
  implicit def value[X <: Primitive](x: Program)    : Value[X]          = Constant[X](x)
  implicit def int(x: scala.Int)                    : Value[Int]        = Constant(runtime.real(x))
  implicit def double(x: scala.Double)              : Value[Double]     = Constant(runtime.real(x))
  implicit def boolean(x: scala.Boolean)            : Value[Boolean]    = Constant(runtime.flag(x))
  implicit def string(x: java.lang.String)          : Value[String]     = Constant(runtime.text(x))

  // Implicit Operations.
  implicit class TextualOps[X <: String](x: Value[X]) {
    def ++[Y <: X](y: Value[X]): Value[String] = add(x, y)
    def quoted: Value[String] = add("\"", add(x, "\""))
    def contains[Y <: X](y: Value[Y]): Value[Boolean] = runtime.contains(x, y)
    def indexOf[Y <: X](y: Value[Y]): Value[Boolean] = runtime.indexOf(x, y)
    def length[Y <: X](): Value[Int] = runtime.length(x)
    def matches[Y <: X](y: Value[String]): Value[Boolean] = runtime.matches(x, y)
    def substring(l: Value[Int], h: Value[Int] = x.length()): Value[String] = slice(x, l, h)
  }

  implicit class ArithmeticOps[X <: Double](x: Value[X]) {
    def unary_- : Value[X] = sub(math.Zero, x)
    def +[Y <: X](y: Value[Y])(implicit evidence: Y <:< X): Value[X] = add(x, y)
    def +[Y >: X <: String](y: Value[Y]): Value[Y] = add(x, y)
    def -[Y <: X](y: Value[Y])(implicit evidence: Y <:< X): Value[X] = sub(x, y)
    def -[Y >: X <: String](y: Value[Y]): Value[Y] = sub(x, y)
    def *[Y <: X](y: Value[Y])(implicit evidence: Y <:< X): Value[X] = mul(x, y)
    def *[Y >: X <: String](y: Value[Y]): Value[Y] = mul(x, y)
    def /[Y <: X](y: Value[Y])(implicit evidence: Y <:< X): Value[X] = div(x, y)
    def /[Y >: X <: String](y: Value[Y]): Value[Y] = div(x, y)
    def %[Y <: X](y: Value[Y])(implicit evidence: Y <:< X): Value[X] = mod(x, y)
    def %[Y >: X <: String](y: Value[Y]): Value[Y] = mod(x, y)
  }

  implicit class ComparisonOps[X <: Primitive](x: Value[X]) {
    def <  [Y <: Primitive](y: Value[Y]): Value[Boolean] = less(x, y)
    def ===[Y <: Primitive](y: Value[Y]): Value[Boolean] = equal(x, y)
    def <= [Y <: Primitive](y: Value[Y]): Value[Boolean] = x < y || x === y
    def >  [Y <: Primitive](y: Value[Y]): Value[Boolean] = !(x <= y)
    def >= [Y <: Primitive](y: Value[Y]): Value[Boolean] = !(x < y)
    def <> [Y <: Primitive](y: Value[Y]): Value[Boolean] = !(x === y)
  }

  implicit class LogicalOps(x: Value[Boolean]) {
    def &&(y: Value[Boolean]): Value[Boolean] = both(x, y)
    def ||(y: Value[Boolean]): Value[Boolean] = either(x, y)
    def unary_! : Value[Boolean] = negate(x)
  }

  implicit class AssignmentOps[T <: Primitive](x: Variable[T]) {
    def :=(y: Value[T])(implicit context: Context): Unit = x.set(y)
  }

  implicit class CompoundAssignmentOps[T <: Double](x: Variable[T]) {
    def +=(y: Value[T])(implicit context: Context): Unit = x := (x + y)
    def -=(y: Value[T])(implicit context: Context): Unit = x := (x - y)
    def *=(y: Value[T])(implicit context: Context): Unit = x := (x * y)
    def /=(y: Value[T])(implicit context: Context): Unit = x := (x / y)
  }

  implicit class ToJsonOps[T <: Primitive](x: Value[T]) {
    def toJson: Value[String] = branch(x <> Null, x, "null")
  }

  implicit class ToJsonStringOps(x: Value[String]) {
    def toJson: Value[String] = branch(x <> Null, x.quoted, "null")
  }


}
