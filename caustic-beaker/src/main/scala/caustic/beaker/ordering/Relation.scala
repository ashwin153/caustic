package caustic.beaker.ordering

import scala.language.implicitConversions

/**
 * An equivalence relation.
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