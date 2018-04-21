package caustic.library
package typing
package collection

import caustic.library.control._

import scala.language.reflectiveCalls

/**
 * A collection of unique values.
 *
 * @param length Current size.
 */
class Set[T <: Primitive](length: Variable[Int]) extends List[T](length) {

  /**
   * Adds the value to the set if and only if it is not already present.
   *
   * @param value Value to add.
   * @param context Parse context.
   * @return Whether or not the value was added.
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
   * Returns a set containing all elements in this set that are not present in the specified set.
   *
   * @param that Set.
   * @param context Parse context.
   * @return Set difference.
   */
  def diff(that: Set[T])(implicit context: Context): Set[T] = {
    val set = Set.Local[T](context.label())
    set ++= this
    set --= that
    set
  }

  /**
   * Returns a set containing all elements present in both sets.
   *
   * @param that Set.
   * @param context Parse context.
   * @return Set intersection.
   */
  def intersect(that: Set[T])(implicit context: Context): Set[T] = {
    val set = Set.Local[T](context.label())
    this foreach { case (_, v) => If (that.contains(v)) { set.add(v) } }
    set
  }

  /**
   * Returns a set containing all elements present in either set.
   *
   * @param that Set.
   * @param context Parse context.
   * @return Set union.
   */
  def union(that: Set[T])(implicit context: Context): Set[T] = {
    val set = Set.Local[T](context.label())
    set ++= this
    set ++= that
    set
  }

}

object Set {

  /**
   * Returns a set backed by the specified variable.
   *
   * @param key Underlying variable.
   * @return Initialized set.
   */
  def apply[T <: Primitive](key: Variable[Int]): Set[T] = new Set(key)

  /**
   * Returns a set backed by the specified local variable.
   *
   * @param key Local variable.
   * @return Local set.
   */
  def Local[T <: Primitive](key: Value[String]): Set[T] = Set(Variable.Local(key))

  /**
   * Returns a set backed by the specified remote variable.
   *
   * @param key Remote variable.
   * @return Remote set.
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
