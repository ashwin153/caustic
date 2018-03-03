package caustic.runtime.typing

import caustic.runtime.Program

/**
 *
 * @param value
 */
case class Constant[X <: Primitive](value: Program) extends Value[X] {

  /**
   *
   * @return
   */
  override def get: Program = this.value

}
