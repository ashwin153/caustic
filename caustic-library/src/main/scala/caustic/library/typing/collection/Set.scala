package caustic.library
package typing
package collection

import caustic.library.control._

import scala.language.reflectiveCalls

/**
 *
 * @param length Current size.
 */
class Set[T <: Primitive](length: Variable[Int]) extends List[T](length) {

  /**
   *
   * @param value
   * @param context
   * @return
   */
  def add(value: Value[T])(implicit context: Context): Value[Boolean] = {
    If (contains(value)) {
      false
    } Else {
      this += value
      true
    }
  }

  /**
   *
   * @param that
   * @param context
   * @return
   */
  def union(that: Set[T])(implicit context: Context): Set[T] = {
    val set = Set.Local[T](context.label())
    set ++= this
    set ++= that
    set
  }

  /**
   *
   * @param that
   * @param context
   * @return
   */
  def intersect(that: Set[T])(implicit context: Context): Set[T] = {
    val set = Set.Local[T](context.label())
    this foreach { case (_, v) => If (that.contains(v)) { set.add(v) } }
    set
  }

  /**
   *
   * @param that
   * @param context
   * @return
   */
  def diff(that: Set[T])(implicit context: Context): Set[T] = {
    val set = Set.Local[T](context.label())
    set ++= this
    set --= that
    set
  }


}

object Set {

  /**
   *
   * @param key
   * @tparam T
   * @return
   */
  def apply[T <: Primitive](key: Variable[Int]): Set[T] = new Set(key)

  /**
   *
   * @param key
   * @return
   */
  def Local[T <: Primitive](key: Value[String]): Set[T] = Set(Variable.Local(key))

  /**
   *
   * @param key
   * @return
   */
  def Remote[T <: Primitive](key: Value[String]): Set[T] = Set(Variable.Remote(key))

  // Implicit Operations.
  implicit class AssignmentOps[T <: Primitive](x: Set[T]) {
    def :=(y: Set[T])(implicit context: Context): Unit = { x.delete(); x ++= y }
  }

  implicit class CompoundAssignmentOps[T <: Primitive](x: Set[T]) {
    def ++=(y: Set[T])(implicit context: Context): Unit = y foreach { case (_, v) => x.add(v) }
    def --=(y: Set[T])(implicit context: Context): Unit = y foreach { case (_, v) => x.remove(v) }
    def +=(y: Value[T])(implicit context: Context): Unit = x.add(y)
    def -=(y: Value[T])(implicit context: Context): Unit = x.remove(y)
  }

}
