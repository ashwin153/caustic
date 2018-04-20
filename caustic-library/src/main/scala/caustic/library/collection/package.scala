package caustic.library

import caustic.library.control.Context
import caustic.library.external.Converter
import caustic.library.typing._
import scala.language.implicitConversions

package object collection {

  // Implicit Conversions.
  implicit def list[A, B <: Primitive](x: scala.List[A])(
    implicit context: Context,
    converter: Converter[A, B]
  ): List[B] = {
    val list = List.Local[B](context.label())
    x.foreach(i => list += converter(i))
    list
  }

  implicit def set[A, B <: Primitive](x: scala.collection.Set[A])(
    implicit context: Context,
    converter: Converter[A, B]
  ): Set[B] = {
    val set = Set.Local[B](context.label())
    x.foreach(i => set += converter(i))
    set
  }

  implicit def map[K0, V0, K1 <: String, V1 <: Primitive](x: scala.collection.Map[K0, V0])(
    implicit context: Context,
    keyConverter: Converter[K0, K1],
    valueConverter: Converter[V0, V1]
  ): Map[K1, V1] = {
    val map = Map.Local[K1, V1](context.label())
    x foreach { case (k, v) => map += keyConverter(k) -> valueConverter(v) }
    map
  }

}
