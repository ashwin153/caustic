package caustic.library

import caustic.library.collection._
import caustic.library.record._
import caustic.library.typing.Value._
import caustic.runtime
import caustic.runtime._
import scala.language.implicitConversions

package object typing {

  val True: Value[Boolean] = runtime.True
  val False: Value[Boolean] = runtime.False

  // Implicit Conversions.
  implicit def program[X <: Primitive](x: Value[X]): Program = x.get
  implicit def value[X <: Primitive](x: Program): Value[X] = Constant[X](x)
  implicit def int(x: scala.Int): Value[Int] = Constant(x)
  implicit def int(x: scala.Long): Value[Int] = Constant(x)
  implicit def double(x: scala.Double): Value[Double] = Constant(x)
  implicit def string(x: java.lang.String): Value[String] = Constant(x)
  implicit def boolean(x: scala.Boolean): Value[Boolean] = Constant(x)

  // Implicit Operations.
  implicit def int2addition(x: scala.Int): AdditionOps[Int] = AdditionOps(x)
  implicit def long2addition(x: scala.Long): AdditionOps[Int] = AdditionOps(x)
  implicit def double2addition(x: scala.Double): AdditionOps[Double] = AdditionOps(x)
  implicit def int2arithmetic(x: scala.Int): ArithmeticOps[Int] = ArithmeticOps(x)
  implicit def long2arithmetic(x: scala.Long): ArithmeticOps[Int] = ArithmeticOps(x)
  implicit def double2arithmetic(x: scala.Double): ArithmeticOps[Double] = ArithmeticOps(x)
  implicit def int2comparison(x: scala.Int): ComparisonOps[Int] = ComparisonOps(x)
  implicit def long2comparison(x: scala.Long): ComparisonOps[Int] = ComparisonOps(x)
  implicit def double2comparison(x: scala.Double): ComparisonOps[Double] = ComparisonOps(x)
  implicit def string2comparison(x: java.lang.String): ComparisonOps[String] = ComparisonOps(x)
  implicit def string2textual(x: java.lang.String): TextualOps[String] = TextualOps(x)
  implicit def boolean2logical(x: scala.Boolean): LogicalOps[Boolean] = LogicalOps(x)

}
