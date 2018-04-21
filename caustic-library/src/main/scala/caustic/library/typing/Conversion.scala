package caustic.library.typing

import caustic.library.Context
import caustic.library.typing.collection._

/**
 * A conversion from Scala to Caustic.
 */
trait Conversion[-Scala, +Caustic] {

  /**
   * Applies the conversion to the specified value.
   *
   * @param x Scala value.
   * @return Caustic value.
   */
  def apply(x: Scala): Caustic

}

object Conversion {

  // scala.Boolean => Boolean
  implicit val boolean: Conversion[scala.Boolean, Value[Boolean]] =
    x => Constant(x)

  // scala.Int => Int
  implicit val int: Conversion[scala.Int, Value[Int]] =
    x => Constant(x)

  // scala.Long => Int
  implicit val long: Conversion[scala.Long, Value[Int]] =
    x => Constant(x)

  // scala.Double => Double
  implicit val double: Conversion[scala.Double, Value[Double]] =
    x => Constant(x)

  // java.lang.String => String
  implicit val string: Conversion[java.lang.String, Value[String]] =
    x => Constant(x)

  // scala.List => List
  implicit def list[S, C <: Primitive](
    implicit context: Context,
    conversion: Conversion[S, Value[C]]
  ): Conversion[scala.List[S], List[C]] = x => {
    val list = List.Local[C](context.label())
    x.foreach(i => list += conversion(i))
    list
  }

  // scala.collection.Set => Set
  implicit def set[S, C <: Primitive](
    implicit context: Context,
    conversion: Conversion[S, Value[C]]
  ): Conversion[scala.collection.Set[S], Set[C]] = x => {
    val set = Set.Local[C](context.label())
    x.foreach(i => set += conversion(i))
    set
  }

  // scala.collection.Map => Map
  implicit def map[K0, V0, K1 <: String, V1 <: Primitive](
    implicit context: Context,
    keyConversion: Conversion[K0, Value[K1]],
    valueConversion: Conversion[V0, Value[V1]]
  ): Conversion[scala.collection.Map[K0, V0], Map[K1, V1]] = x => {
    val map = Map.Local[K1, V1](context.label())
    x foreach { case (k, v) => map += keyConversion(k) -> valueConversion(v) }
    map
  }

}