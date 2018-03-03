package caustic.beaker.ordering

import scala.language.implicitConversions

/**
 * An equivalence relation. Relations are reflexive (x ~ x), symmetric (x ~ y -> y ~ x), and
 * transitive (x ~ y, y ~ z -> x ~ z). Relations partition a set into equivalence classes.
 */
trait Relation[-T] {

  def equiv(x: T, y: T): Boolean

}

object Relation {

  // Total relations map all elements to the same equivalence class.
  val Total: Relation[Any] = (x, y) => true

  // Identity relations map each element to their own equivalence class.
  val Identity: Relation[Any] = (x, y) => x == y

}