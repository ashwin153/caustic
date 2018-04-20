package caustic.library

import caustic.library.collection._
import caustic.library.record._
import caustic.library.typing._

import scala.language.implicitConversions

package object external {

  implicit def reference[T, U](x: Pointer[T]): Reference[U] = Reference.Remote(x.key)
  implicit def primitive[T <: Primitive](x: Pointer[Primitive]): Variable[T] = Variable.Remote(x.key)
  implicit def list[T <: Primitive](x: Pointer[List[T]]): List[T] = List.Remote(x.key)
  implicit def set[T <: Primitive](x: Pointer[Set[T]]): Set[T] = Set.Remote(x.key)
  implicit def map[A <: String, B <: Primitive](x: Pointer[Map[A, B]]): Map[A, B] = Map.Remote(x.key)

}
