package caustic.runtime.collection

import caustic.runtime.control._
import caustic.runtime.typing._

/**
 *
 * @param size
 * @tparam T
 */
case class Collection[T](size: Variable[Int]) {

//  /**
//   *
//   * @param index
//   * @return
//   */
//  def get(index: Value[Int])(
//    implicit context: Context,
//    constructor: Constructor[T]
//  ): T = constructor.construct(this.size.scope(index).watch(this.exists.set))

}
