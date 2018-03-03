package caustic.runtime.typing

/**
 *
 */
sealed trait Primitive
trait Null extends Primitive
trait String extends Null
trait Double extends String
trait Int extends Double
trait Boolean extends Int
