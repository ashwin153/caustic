package caustic.compiler.check

/**
 * A typed expression.
 *
 * @param tag Type tag.
 * @param value Contents.
 */
case class Result(tag: Type, value: String)