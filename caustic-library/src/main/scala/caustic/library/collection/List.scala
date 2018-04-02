package caustic.library.collection

import caustic.library.control._
import caustic.library.typing._
import caustic.runtime.Null

/**
 * A dynamically-sized collection of scalar values.
 *
 * @param size Current size.
 */
case class List[T <: Primitive](size: Variable[Int]) {

  /**
   * Returns the value at the specified index.
   *
   * @param index Index.
   * @return Value at index.
   */
  def get(index: Value[Int]): Value[T] = size.scope(index)

  /**
   * Sets the value at the specified index.
   *
   * @param index Index.
   * @param value Updated value.
   * @param context Parse context.
   */
  def set(index: Value[Int], value: Value[T])(implicit context: Context): Unit = {
    // Grow the list if a larger index is added.
    If (index >= this.size) {
      this.size := index
    }

    // Shrink the list if the last index is deleted.
    If (index === this.size && value === Null) {
      this.size -= 1
    }

    // Set the value of the specified index.
    this.size.scope[T](index) := value
  }

  /**
   * Returns whether or not the list contains the value.
   *
   * @param value Value to lookup.
   * @param context Parse context.
   * @return Whether or not the list contains the value.
   */
  def contains(value: Value[T])(implicit context: Context): Value[Boolean] = {
    indexOf(value) >= 0
  }

  /**
   * Returns the index of the specified value.
   *
   * @param value Value to lookup.
   * @param context Parse context.
   * @return Whether or not the list contains the value.
   */
  def indexOf(value: Value[T])(implicit context: Context): Value[Int] = {
    val index = this.size.scope[Int]("$i")
    index := this.size - 1

    // Iterate through the list until the value is found.
    While (index >= 0 && get(index) <> value) {
      index -= 1
    }

    index
  }

  /**
   * Returns whether or not the lists contain the same elements.
   *
   * @param that Another list.
   * @param context Parse context.
   * @return Whether or not the lists are equal.
   */
  def ===(that: List[T])(implicit context: Context): Value[Boolean] = {
    val index = this.size.scope[Int]("$i")
    index := 0

    // Iterate through both lists and verify that all values are the same.
    While (index < this.size && index < that.size && get(index) === that.get(index)) {
      index += 1
    }

    this.size === that.size && index <> this.size
  }

  /**
   * Applies the function to each element of the list.
   *
   * @param f Function to apply.
   * @param context Parse context.
   */
  def foreach[U](f: (Value[Int], Value[T]) => U)(implicit context: Context): Unit = {
    val index = this.size.scope[Int]("$i")
    index := 0

    // Iterate through the list.
    While (index < this.size) {
      f(index, get(index))
    }
  }

}
