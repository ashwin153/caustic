package caustic.library.typing

/**
 * A scalar type.
 */
sealed trait Primitive
trait String extends Primitive
trait Double extends String
trait Int extends Double
trait Boolean extends Int