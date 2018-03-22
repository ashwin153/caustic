package caustic.runtime.collection

import caustic.runtime.typing._
import shapeless._
import shapeless.ops.record.Selector

/**
 *
 * @tparam T
 */
case class Object[T](key: Variable[String]) {

  /**
   *
   * @param witness
   * @param gen
   * @param selector
   * @param constructor
   * @return
   */
  def get[Repr <: HList, Field <: Symbol, Type](witness: Witness.Lt[Field])(
    implicit context: Context,
    constructor: Constructor[Type],
    gen: LabelledGeneric.Aux[T, Repr],
    selector: Selector.Aux[Repr, Field, Type]
  ): Type = constructor.construct(this.key.scope("@@" ++ witness.value.name))

  /**
   *
   * @return
   */
  def delete(
    implicit context: Context
  ): Unit = ???

}
