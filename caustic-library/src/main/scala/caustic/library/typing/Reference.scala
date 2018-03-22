package caustic.library.typing

import shapeless.ops.record.Selector
import shapeless.{HList, LabelledGeneric, Witness}

/**
 * An object reference.
 */
class Reference(key: Variable[String]) {

  type To

  /**
   * Returns a variable containing the value of the specified field.
   *
   * @param witness Field name.
   * @param gen Generic representation.
   * @param selector Field selector.
   * @return Variable containing scalar value.
   */
  def get[Repr <: HList, Name <: Symbol, Field <: Primitive](witness: Witness.Lt[Name])(
    implicit gen: LabelledGeneric.Aux[To, Repr],
    selector: Selector.Aux[Repr, Name, Variable[Field]]
  ): Variable[Field] = this.key.scope(witness.value.name)

  /**
   * Returns a reference to the referenced object with the specified name.
   *
   * @param witness Field name.
   * @param gen Generic representation.
   * @param selector Field selector.
   * @return Reference to referenced object.
   */
  def get[Repr <: HList, Name <: Symbol, Field <: Reference](witness: Witness.Lt[Name])(
    implicit gen: LabelledGeneric.Aux[To, Repr],
    selector: Selector.Aux[Repr, Name, Variable[Field]]
  ): Reference.To[Field] = this.key match {
    case Variable.Local(_) => Reference(Variable.Local(this.key.scope(witness.value.name).value))
    case Variable.Remote(_) => Reference(Variable.Remote(this.key.scope(witness.value.name).value))
  }

  /**
   * Returns a reference to the nested object with the specified name.
   *
   * @param witness Field name.
   * @param gen Generic representation.
   * @param selector Field selector.
   * @return Reference to nested object.
   */
  def get[Repr <: HList, Name <: Symbol, Field](witness: Witness.Lt[Name])(
    implicit gen: LabelledGeneric.Aux[To, Repr],
    selector: Selector.Aux[Repr, Name, Reference.To[Field]]
  ): Reference.To[Field] = Reference(this.key.scope(witness.value.name))

}

object Reference {

  type To[T] = Reference { type To = T }

  /**
   * Constructs a reference to the object stored at the specified key.
   *
   * @param key Reference key.
   * @return Object reference.
   */
  def apply[T](key: Variable[String]): Reference.To[T] = new Reference.To[T](key)

}