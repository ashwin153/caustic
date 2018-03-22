package caustic.library.typing

import shapeless.ops.record.Selector
import shapeless.{HList, LabelledGeneric, Witness}

/**
 * An object reference.
 */
case class Reference[T](key: Variable[String]) {

  /**
   * Returns a variable corresponding to the specified scalar field of the object.
   *
   * @param witness Field name.
   * @param gen Generic representation.
   * @param selector Field selector.
   * @return Variable containing the field.
   */
  def get[Repr <: HList, Name <: Symbol, Field <: Primitive](witness: Witness.Lt[Name])(
    implicit gen: LabelledGeneric.Aux[T, Repr],
    selector: Selector.Aux[Repr, Name, Variable[Field]]
  ): Variable[Field] = this.key.scope(witness.value.name)

  /**
   * Returns a variable corresponding to the specified field of the object.
   *
   * @param witness Field name.
   * @param gen Generic representation.
   * @param selector Field selector.
   * @return Reference to the field.
   */
  def get[Repr <: HList, Name <: Symbol, Field](witness: Witness.Lt[Name])(
    implicit gen: LabelledGeneric.Aux[T, Repr],
    selector: Selector.Aux[Repr, Name, Reference[Field]]
  ): Reference[Field] = Reference(this.key.scope(witness.value.name))

}
