package caustic.library.typing

import caustic.runtime.Program

/**
 * A constant value.
 *
 * @param get Program representation.
 */
case class Constant[+T <: Primitive](get: Program) extends Value[T]