package caustic.compiler.error

/**
 *
 * @param message
 */
case class TypeError(message: String) extends Exception(message)