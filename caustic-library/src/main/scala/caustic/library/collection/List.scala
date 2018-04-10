package caustic.library.collection

import caustic.library.control._
import caustic.library.typing._
import caustic.library.typing.Value._
import caustic.runtime._

import scala.language.reflectiveCalls

/**
 * A mutable collection of values.
 *
 * @param length Current size.
 */
case class List[T <: Primitive](length: Variable[Int]) {

  /**
   * Returns the number of values in the list.
   *
   * @return Number of values.
   */
  def size: Value[Int] = this.length

  /**
   * Returns the value at the specified index.
   *
   * @param index Index.
   * @param context Parse context.
   * @return Value.
   */
  def apply(index: Value[Int])(implicit context: Context): Variable[T] = {
    this.length.scope[T](index)
  }

  /**
   * Appends the value to the end of the list.
   *
   * @param value Value.
   * @param context Parse context.
   */
  def append(value: Value[T])(implicit context: Context): Unit = {
    this.length.scope[T](this.length) := value
    this.length += 1
  }

  /**
   * Applies the function to each value in the list.
   *
   * @param f Function.
   * @param context Parse context.
   */
  def foreach[U](f: (Value[Int], Value[T]) => U)(implicit context: Context): Unit = {
    // Prefetch all items if the list is remotely stored.
    if (this.length.isInstanceOf[Variable.Remote[Int]])
      context += prefetch(this.length.key, this.length, 1)

    // Iterate through each item in the list.
    val index = Variable.Local[Int](context.label())
    index := 0

    While (index < this.length) {
      f(index, this(index))
    }
  }

  /**
   * Removes the value from the list.
   *
   * @param value Value.
   * @param context Parse context.
   */
  def remove(value: Value[T])(implicit context: Context): Unit = {
    val index = this.indexOf(value)

    If (index <> -1) {
      // Shift over all values after the index of the removed value.
      foreach { case (i, _) =>
        If (i >= index && i < this.length - 1) {
          this.length.scope[T](i) := this(i + 1)
        }
      }

      // Decrement the size of the list.
      this.length.scope[T](this.length - 1) := None
      this.length -= 1
    }
  }

  /**
   * Returns whether or not the list contains the specified value.
   *
   * @param value Value.
   * @param context Parse context.
   * @return Whether or not the value is in the list.
   */
  def contains(value: Value[T])(implicit context: Context): Value[Boolean] = {
    indexOf(value) >= 0
  }

  /**
   * Returns the index of the value in the list of -1 if it is not present.
   *
   * @param value Value.
   * @param context Parse context.
   * @return Index of value in list or -1.
   */
  def indexOf(value: Value[T])(implicit context: Context): Value[Int] = {
    val index = Variable.Local[Int](context.label())
    index := -1
    foreach { case (i, v) => If (index === -1 && v === value)(index := i) }
    index
  }

  /**
   * Returns whether or not the lists contain the same values.
   *
   * @param that Another list.
   * @param context Parse context.
   * @return Whether or not the lists contain the same values.
   */
  def ===(that: List[T])(implicit context: Context): Value[Boolean] = {
    val equal = Variable.Local[Boolean](context.label())
    equal := true
    foreach { case (i, v) => equal := equal && this(i) === v }
    this.size === that.size && equal
  }

  /**
   * Removes all values from the list.
   *
   * @param context Parse context.
   */
  def clear()(implicit context: Context): Unit = {
    foreach { case (i, _) => this.length.scope[T](i) := None }
    this.length := 0
  }

  /**
   * Returns the contents of the list as a JSON string.
   *
   * @param context Parse context.
   * @return JSON representation.
   */
  def asJson(implicit context: Context): Value[String] = {
    val json = Variable.Local[String](context.label())
    json := "["

    foreach { case (_, v) =>
      If (json === "[") {
        json := json + v.asJson
      } Else {
        json := json + ", " + v.asJson
      }
    }

    json + "]"
  }

  /**
   * Returns the list as a set.
   *
   * @return Set.
   */
  def toSet: Set[T] = new Set(this)

}

object List {

  // Implicit Operations.
  implicit class AssignmentOps[T <: Primitive](x: List[T]) {
    def :=(y: List[T])(implicit context: Context): Unit = { x.clear(); x ++= y }
    def :=(y: Set[T])(implicit context: Context): Unit = { x.clear(); x ++= y }
  }

  implicit class CompoundAssignmentOps[T <: Primitive](x: List[T]) {
    def ++=(y: List[T])(implicit context: Context): Unit = y foreach { case (_, v) => x += _ }
    def --=(y: List[T])(implicit context: Context): Unit = y foreach { case (_, v) => x -= _ }
    def ++=(y: Set[T])(implicit context: Context): Unit = y.foreach(x += _)
    def --=(y: Set[T])(implicit context: Context): Unit = y.foreach(x -= _)
    def +=(y: Value[T])(implicit context: Context): Unit = x.append(y)
    def -=(y: Value[T])(implicit context: Context): Unit = x.remove(y)
  }

}