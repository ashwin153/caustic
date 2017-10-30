package caustic.compiler

/**
 * A compiler error.
 *
 * @param code Unique identifier.
 * @param name Title.
 * @param description Message text.
 */
case class Error(code: Int, name: String, description: String)

/**
 * A compiler warning.
 *
 * @param code Unique identifier.
 * @param name Title.
 * @param description Message text.
 */
case class Warning(code: Int, name: String, description: String)
