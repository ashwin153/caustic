package caustic.runtime

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

}
