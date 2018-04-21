package caustic.library

import caustic.library.typing.Value._
import caustic.library.typing.collection.{List, Map, Set}
import caustic.library.typing.record.Reference
import caustic.runtime._
import scala.language.implicitConversions

package object typing {

  val True: Value[Boolean] = caustic.runtime.True
  val False: Value[Boolean] = caustic.runtime.False

  // Implicit Conversions.
  implicit def program[X <: Primitive](x: Value[X]): Program = x.get
  implicit def value[X <: Primitive](x: Program): Value[X] = Constant[X](x)
  implicit def convert[S, C](x: S)(implicit conversion: Conversion[S, C]): C = conversion(x)
  implicit def reference[T, U](x: Pointer[T]): Reference[U] = Reference.Remote(x.key)
  implicit def variable[T <: Primitive](x: Pointer[T]): Variable[T] = Variable.Remote(x.key)
  implicit def list[T <: Primitive](x: Pointer[List[T]]): List[T] = List.Remote(x.key)
  implicit def set[T <: Primitive](x: Pointer[Set[T]]): Set[T] = Set.Remote(x.key)
  implicit def map[K <: String, V <: Primitive](x: Pointer[Map[K, V]]): Map[K, V] = Map.Remote(x.key)

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
