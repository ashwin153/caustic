package caustic.runtime.collection

import caustic.lang.{Value, caustic.runtime.control}
import caustic.runtime.control.Context
import caustic.runtime.typing.{Int, Value, Variable, caustic, collection, lang, ops}

/**
 *
 * @param size
 * @tparam T
 */
case class Collection[T](size: Variable[Int]) {

  /**
   *
   * @param index
   * @return
   */
  def get(index: Value[Int])(
    implicit context: Context,
    constructor: ops.Constructor[T]
  ): T = constructor.construct(this.size.scope(index).watch(this.exists.set))

}
