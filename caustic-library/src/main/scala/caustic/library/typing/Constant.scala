package caustic.library.typing

import caustic.runtime.Program

/**
 * A constant value.
 *
 * @param get Program representation.
 */
case class Constant[X <: Primitive](get: Program) extends Value[X]