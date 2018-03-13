package caustic.beaker.common

import scala.language.implicitConversions

/**
 * A binary relation. Relations are symmetric (x ~ y -> y ~ x), but may not necessarily be reflexive
 * (x ~ x) or transitive (x ~ y, y ~ z -> x ~ z).
 */
trait Relation[-T] {

  def related(x: T, y: T): Boolean

}

object Relation {

  // Total relations map all elements to the same equivalence class.
  val Total: Relation[Any] = (x, y) => true

  // Identity relations map each element to their own equivalence class.
  val Identity: Relation[Any] = (x, y) => x == y

}