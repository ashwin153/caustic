package caustic.library.typing
package record

import caustic.library.typing.collection._

import shapeless._

/**
 *
 * @tparam Type
 */
trait Field[Type] {
  type Container
  def apply(key: Variable[String], field: Value[String]): Container
  def apply[T](ref: Reference[T], field: Value[String]): Container = apply(ref.pointer, field)
}

object Field {

  type Aux[Type, Container0] = Field[Type] { type Container = Container0 }

  // Implicit Conversions.
  implicit def value[T <: Primitive]: Aux[Value[T], Variable[T]] = new Field[Value[T]] {
    type Container = Variable[T]
    override def apply(k: Variable[String], f: Value[String]): Variable[T] = k.scope(f)
  }

  implicit def reference[T]: Aux[Reference[T], Reference[T]] = new Field[Reference[T]] {
    override type Container = Reference[T]
    override def apply(k: Variable[String], f: Value[String]): Reference[T] = Reference(k.scope(f))
  }

  implicit def record[T](implicit evidence: T <:!< Internal): Aux[T, Reference[T]] = new Field[T] {
    override type Container = Reference[T]
    override def apply(k: Variable[String], f: Value[String]): Reference[T] = Reference(k.scope(f))
  }

  implicit def list[T <: Primitive]: Aux[List[T], List[T]] = new Field[List[T]] {
    override type Container = List[T]
    override def apply(k: Variable[String], f: Value[String]): List[T] = List(k.scope(f))
  }

  implicit def set[T <: Primitive]: Aux[Set[T], Set[T]] = new Field[Set[T]] {
    override type Container = Set[T]
    override def apply(k: Variable[String], f: Value[String]): Set[T] = Set(k.scope[Int](f))
  }

  implicit def map[A <: String, B <: Primitive]: Aux[Map[A, B], Map[A, B]] = new Field[Map[A, B]] {
    override type Container = Map[A, B]
    override def apply(k: Variable[String], f: Value[String]): Map[A, B] = Map(k.scope[Int](f))
  }

}