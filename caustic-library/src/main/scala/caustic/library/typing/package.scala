package caustic.library

import caustic.library.control.Context
import caustic.runtime
import caustic.runtime._
import scala.language.implicitConversions

package object typing {

  // Implicit Conversions.
  implicit def convert(x: Value[_])                 : Program         = x.get
  implicit def convert[X <: Primitive](x: Program)  : Value[X]        = Constant[X](x)
  implicit def convert(x: scala.Int)                : Value[Int]      = Constant(runtime.real(x))
  implicit def convert(x: scala.Double)             : Value[Double]   = Constant(runtime.real(x))
  implicit def convert(x: scala.Boolean)            : Value[Boolean]  = Constant(runtime.flag(x))
  implicit def convert(x: java.lang.String)         : Value[String]   = Constant(runtime.text(x))

  // Implicit Operations.
  implicit class Textual[X <: String](x: Value[X]) {
    def ++[Y <: X](y: Value[X]): Value[String] = add(x, y)
    def contains[Y <: X](y: Value[Y]): Value[Boolean] = runtime.contains(x, y)
    def indexOf[Y <: X](y: Value[Y]): Value[Boolean] = runtime.indexOf(x, y)
    def length[Y <: X](): Value[Int] = runtime.length(x)
    def matches[Y <: X](y: Value[String]): Value[Boolean] = runtime.matches(x, y)
    def substring(l: Value[Int], h: Value[Int] = x.length()): Value[String] = slice(x, l, h)
  }

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

  implicit class Comparison[X <: Primitive](x: Value[X]) {
    def <  [Y <: Primitive](y: Value[Y]): Value[Boolean] = less(x, y)
    def ===[Y <: Primitive](y: Value[Y]): Value[Boolean] = equal(x, y)
    def <= [Y <: Primitive](y: Value[Y]): Value[Boolean] = x < y || x === y
    def >  [Y <: Primitive](y: Value[Y]): Value[Boolean] = !(x <= y)
    def >= [Y <: Primitive](y: Value[Y]): Value[Boolean] = !(x < y)
    def <> [Y <: Primitive](y: Value[Y]): Value[Boolean] = !(x === y)
  }

  implicit class Logical(x: Value[Boolean]) {
    def &&(y: Value[Boolean]): Value[Boolean] = both(x, y)
    def ||(y: Value[Boolean]): Value[Boolean] = either(x, y)
    def unary_! : Value[Boolean] = negate(x)
  }

  implicit class Assignment[T <: Primitive](x: Variable[T]) {
    def :=(y: Value[T])(implicit context: Context): Unit = x.set(y)
  }

  implicit class CompoundAssignment[T <: Double](x: Variable[T]) {
    def +=(y: Value[T])(implicit context: Context): Unit = x := (x + y)
    def -=(y: Value[T])(implicit context: Context): Unit = x := (x - y)
    def *=(y: Value[T])(implicit context: Context): Unit = x := (x * y)
    def /=(y: Value[T])(implicit context: Context): Unit = x := (x / y)
  }

}
