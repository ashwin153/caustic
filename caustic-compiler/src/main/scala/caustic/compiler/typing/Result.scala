package caustic.compiler.typing

/**
 * A type-tagged value.
 *
 * @param kind Type.
 * @param value Code.
 */
case class Result(kind: Kind, value: String)
