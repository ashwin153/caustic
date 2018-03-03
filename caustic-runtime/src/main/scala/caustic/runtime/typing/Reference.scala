package caustic.runtime.typing

import shapeless.ops.record.Selector
import shapeless.{HList, LabelledGeneric, Witness}

/**
 *
 * @tparam T
 */
case class Reference[T](key: Variable[String]) {

  /**
   *
   * @param witness
   * @param gen
   * @param selector
   * @return
   */
  def get[Repr <: HList, Name <: Symbol, Field <: Primitive](witness: Witness.Lt[Name])(
    implicit gen: LabelledGeneric.Aux[T, Repr],
    selector: Selector.Aux[Repr, Name, Variable[Field]]
  ): Variable[Field] = this.key.scope(witness.value.name)

  /**
   *
   * @param witness
   * @param gen
   * @param selector
   * @return
   */
  def get[Repr <: HList, Name <: Symbol, Field](witness: Witness.Lt[Name])(
    implicit gen: LabelledGeneric.Aux[T, Repr],
    selector: Selector.Aux[Repr, Name, Reference[Field]]
  ): Reference[Field] = Reference(this.key.scope(witness.value.name))

}
