package caustic.library.typing

import caustic.runtime.Program

/**
 * A scalar value.
 */
trait Value[+T <: Primitive] {

  /**
   * Returns the value as a program.
   *
   * @return Program representation.
   */
  def get: Program

}