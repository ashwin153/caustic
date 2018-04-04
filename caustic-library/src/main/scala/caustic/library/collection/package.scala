package caustic.library

import caustic.library.control.Context
import caustic.library.typing._
import caustic.runtime.Null

package object collection {

  // Implicit Operations.
  implicit class ListOps[T <: Primitive](x: List[T]) {
    def ++=(y: List[T])(implicit context: Context): Unit = y foreach { case (_, v) => x += _ }
    def --=(y: List[T])(implicit context: Context): Unit = y foreach { case (_, v) => x -= _ }
    def ++=(y: Set[T])(implicit context: Context): Unit = y.foreach(x += _)
    def --=(y: Set[T])(implicit context: Context): Unit = y.foreach(x -= _)
    def +=(y: Value[T])(implicit context: Context): Unit = x.append(y)
    def -=(y: Value[T])(implicit context: Context): Unit = x.remove(y)
    def :=(y: List[T])(implicit context: Context): Unit = { x.clear(); x ++= y }
    def :=(y: Set[T])(implicit context: Context): Unit = { x.clear(); x ++= y }
  }

  implicit class SetOps[T <: Primitive](x: Set[T]) {
    def ++=(y: List[T])(implicit context: Context): Unit = y foreach { case (_, v) => x += v }
    def --=(y: List[T])(implicit context: Context): Unit = y foreach { case (_, v) => x -= v }
    def ++=(y: Set[T])(implicit context: Context): Unit = y.foreach(x += _)
    def --=(y: Set[T])(implicit context: Context): Unit = y.foreach(x -= _)
    def +=(y: Value[T])(implicit context: Context): Unit = x.add(y)
    def -=(y: Value[T])(implicit context: Context): Unit = x.remove(y)
    def :=(y: List[T])(implicit context: Context): Unit = { x.clear(); x ++= y }
    def :=(y: Set[T])(implicit context: Context): Unit = { x.clear(); x ++= y }
  }

  implicit class MapOps[A <: String, B <: Primitive](x: Map[A, B]) {
    def ++=(y: Map[A, B])(implicit context: Context): Unit = y.foreach(x.put)
    def --=(y: Map[A, B])(implicit context: Context): Unit = y foreach { case (k, _) => x -= k }
    def +=(k: Value[A], v: Value[B])(implicit context: Context): Unit = x.put(k, v)
    def -=(k: Value[A])(implicit context: Context): Unit = x.put(k, Null)
    def :=(y: Map[A, B])(implicit context: Context): Unit = { x.clear(); x ++= y }
  }

}
