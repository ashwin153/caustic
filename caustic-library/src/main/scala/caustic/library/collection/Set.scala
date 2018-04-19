package caustic.library.collection

import caustic.library.Internal
import caustic.library.control._
import caustic.library.typing._

import scala.language.reflectiveCalls

/**
 * A mutable collection of unique values.
 *
 * @param toList Underlying list.
 */
case class Set[T <: Primitive](toList: List[T]) extends Internal {

  /**
   * Returns the number of elements in the set.
   *
   * @return Number of elements.
   */
  def size: Value[Int] = this.toList.size

  /**
   * Adds the value to the set if it is not already present.
   *
   * @param value Value.
   * @param context Parse context.
   * @return Whether or not the value was added.
   */
  def add(value: Value[T])(implicit context: Context): Value[Boolean] = {
    If (contains(value)) {
      false
    } Else {
      this.toList.append(value)
      true
    }
  }

  /**
   * Returns whether or not the set contains the value.
   *
   * @param value Value.
   * @param context Parse context.
   * @return Whether or not the set contains the value.
   */
  def contains(value: Value[T])(implicit context: Context): Value[Boolean] = {
    this.toList.contains(value)
  }

  /**
   * Removes the value from the set.
   *
   * @param value Value.
   * @param context Parse context.
   */
  def remove(value: Value[T])(implicit context: Context): Unit = {
    this.toList.remove(value)
  }

  /**
   * Applies the function to each element in the set.
   *
   * @param f Function.
   * @param context Parse context.
   */
  def foreach[U](f: Value[T] => U)(implicit context: Context): Unit = {
    this.toList foreach { case (_, v) => f(v) }
  }

  /**
   * Removes all elements from the set.
   *
   * @param context Parse context.
   */
  def delete()(implicit context: Context): Unit = this.toList.delete()

  /**
   * Returns whether or not the sets contain the same elements.
   *
   * @param that Another set.
   * @param context Parse context.
   */
  def ===(that: Set[T])(implicit context: Context): Value[Boolean] = this.toList === that.toList

  /**
   * Returns the contents of the set as a JSON string.
   *
   * @param context Parse context.
   * @return JSON representation.
   */
  def asJson(implicit context: Context): Value[String] = this.toList.asJson

}

object Set {

  /**
   *
   * @param key
   * @tparam T
   * @return
   */
  def apply[T <: Primitive](key: Variable[Int]): Set[T] = Set(List[T](key))

  /**
   *
   * @param key
   * @return
   */
  def Local[T <: Primitive](key: Value[String]): Set[T] = Set(List.Local[T](key))

  /**
   *
   * @param key
   * @return
   */
  def Remote[T <: Primitive](key: Value[String]): Set[T] = Set(List.Remote[T](key))

  // Implicit Operations.
  implicit class AssignmentOps[T <: Primitive](x: Set[T]) {
    def :=(y: List[T])(implicit context: Context): Unit = { x.delete(); x ++= y }
    def :=(y: Set[T])(implicit context: Context): Unit = { x.delete(); x ++= y }
  }

  implicit class CompoundAssignmentOps[T <: Primitive](x: Set[T]) {
    def ++=(y: List[T])(implicit context: Context): Unit = y foreach { case (_, v) => x += v }
    def --=(y: List[T])(implicit context: Context): Unit = y foreach { case (_, v) => x -= v }
    def ++=(y: Set[T])(implicit context: Context): Unit = y.foreach(x += _)
    def --=(y: Set[T])(implicit context: Context): Unit = y.foreach(x -= _)
    def +=(y: Value[T])(implicit context: Context): Unit = x.add(y)
    def -=(y: Value[T])(implicit context: Context): Unit = x.remove(y)
  }

}
