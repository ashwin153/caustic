package caustic.compiler

/**
 *
 * @param code
 * @param title
 */
sealed abstract class Error(val code: Int, val title: String)

/**
 *
 */
case object UnknownError extends Error(0, "Unknown Error")

/**
 *
 */
case object SyntaxError extends Error(1, "Syntax Error")

/**
 *
 */
case object TypeError extends Error(2, "Type Mismatch")

/**
 *
 */
sealed trait Warning

/**
 *
 */
case object NameShadowing extends Error(0, "Name Shadowing") with Warning