package caustic.library.record

import caustic.library.typing._

/**
 * An attribute extractor for a record.
 */
trait Field[Type] {
  type Container
  def apply(key: Variable[String], field: Value[String]): Container
  def apply[T](ref: Reference[T], field: Value[String]): Container = apply(ref.pointer, field)
}

object Field extends LowPriorityField {

  // Scalar fields are contained in mutable variables.
  implicit def scalar[T <: Primitive]: Aux[T, Variable[T]] = new Field[T] {
    type Container = Variable[T]
    override def apply(key: Variable[String], field: Value[String]): Variable[T] = key.scope(field)
  }

  // Pointers are dereferenced and contained in references.
  implicit def pointer[T]: Aux[Reference[T], Reference[T]] = new Field[Reference[T]] {
    override type Container = Reference[T]
    override def apply(key: Variable[String], field: Value[String]): Reference[T] = Reference(key.scope(field))
  }
}

trait LowPriorityField {

  type Aux[Type, Container0] = Field[Type] { type Container = Container0 }

  // References are contained in references.
  implicit def nested[T]: Aux[T, Reference[T]] = new Field[T] {
    override type Container = Reference[T]
    override def apply(key: Variable[String], field: Value[String]): Reference[T] = Reference(key.scope(field))
  }

}