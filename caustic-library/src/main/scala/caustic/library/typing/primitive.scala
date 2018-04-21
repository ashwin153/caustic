package caustic.library.typing

/**
 * A scalar type.
 */
sealed trait Primitive extends Internal
trait String extends Primitive
trait Double extends String
trait Int extends Double
trait Boolean extends Int