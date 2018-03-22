package caustic.library.typing

import caustic.library.typing.Reference._

import shapeless.ops.record.Selector
import shapeless._

/**
 * An object reference.
 */
case class Reference[T](key: Variable[String]) {

  /**
   * Returns a container that stores the value of the attribute with the specified name.
   *
   * @param witness Attribute name.
   * @param gen Generic representation.
   * @param selector Attribute selector.
   * @param field Field converter.
   * @return Container for the value of the field.
   */
  def get[Repr <: HList, Name <: Symbol, Type, Container](witness: Witness.Lt[Name])(
    implicit gen: LabelledGeneric.Aux[T, Repr],
    selector: Selector.Aux[Repr, Name, Type],
    field: Field.Aux[Type, Container]
  ): Container = field(this.key, witness.value.name)

}

object Reference {

  /**
   * An object field. Fields have a static type and a container for their dynamic value.
   */
  trait Field[Type] {
    type Container
    def apply(key: Variable[String], field: Value[String]): Container
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
      override def apply(key: Variable[String], field: Value[String]): Reference[T] = key match {
        case Variable.Local(_) => Reference(Variable.Local(key.scope(field).value))
        case Variable.Remote(_) => Reference(Variable.Remote(key.scope(field).value))
      }

    }
  }

  trait LowPriorityField {

    type Aux[Type, Container0] = Field[Type] { type Container = Container0 }

    // References are contained in references.
    implicit def reference[T]: Aux[T, Reference[T]] = new Field[T] {
      override type Container = Reference[T]
      override def apply(key: Variable[String], field: Value[String]): Reference[T] = Reference(key.scope(field))
    }

  }

}