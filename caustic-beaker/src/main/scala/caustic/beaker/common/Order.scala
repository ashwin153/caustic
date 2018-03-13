package caustic.beaker.common

import scala.language.implicitConversions

/**
 * A partial ordering. Orders are reflexive (x <| x) and anti-symmetric (x <| y -> y |> x), but may
 * not necessarily be transitive (x <| y, y <| z -> x <| z). Orders induce a reflexive [[Relation]]
 * on comparable elements.
 */
trait Order[-T] extends Relation[T] {

  def before(x: T, y: T): Option[Boolean]

  override def related(x: T, y: T): Boolean = {
    before(x, y).exists(identity) || before(y, x).exists(identity)
  }

}