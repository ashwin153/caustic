package caustic.library.collection

import caustic.library.control._
import caustic.library.typing._
import caustic.runtime.Null

/**
 * A dynamically-sized collection of unique scalar values.
 *
 * @param list Underlying list.
 */
case class Set[T <: Primitive](list: List[T]) {

  /**
   * Returns the number of elements in the set.
   *
   * @return Number of elements.
   */
  def size: Value[Int] = this.list.size

  /**
   * Returns whether or not the set contains the specified value.
   *
   * @param value Value to lookup.
   * @param context Parse context.
   * @return Whether or not the value is in the set.
   */
  def contains(value: Value[T])(implicit context: Context): Value[Boolean] = {
    this.list.contains(value)
  }

  /**
   * Returns whether or not the value was removed.
   *
   * @param value Value to remove.
   * @param context Parse context.
   * @return Whether or not the value was removed.
   */
  def remove(value: Value[T])(implicit context: Context): Value[Boolean] = {
    If (contains(value)) {
      this.list.set(this.list.indexOf(value), Null)
      true
    } Else {
      false
    }
  }

  /**
   * Applies the function to each element in the set.
   *
   * @param f Function to apply.
   * @param context Parse context.
   */
  def foreach[U](f: Value[T] => U)(implicit context: Context): Unit = {
    this.list foreach { case (_, v) => f(v) }
  }

  /**
   * Adds the value to the set if it doesn't already contain it.
   *
   * @param value Value to add.
   * @param context Parse context.
   * @return Whether or not the value was added.
   */
  def add(value: Value[T])(implicit context: Context): Value[Boolean] = {
    If (contains(value)) {
      false
    } Else {
      this.list.set(this.list.size, value)
      true
    }
  }

}
