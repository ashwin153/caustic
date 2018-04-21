package caustic.library.typing
package collection

import caustic.library.Context
import caustic.library.control._

import caustic.runtime.Null

/**
 * An indexed collection of values.
 */
trait Collection[K <: Primitive, V <: Primitive] extends Internal {

  /**
   * Returns the value at the specified index.
   *
   * @param key Index.
   * @param context Parse context.
   * @return Corresponding value or [[Null]].
   */
  def apply(key: Value[K])(implicit context: Context): Value[V] = get(key)

  /**
   * Serializes the collection to a JSON string.
   *
   * @param context Parse context.
   * @return Serialized representation.
   */
  def asJson(implicit context: Context): Value[String]

  /**
   * Returns whether or not the collection contains the specified value.
   *
   * @param value Value.
   * @param context Parse context.
   * @return Whether or not the value is in the collection.
   */
  def contains(value: Value[V])(implicit context: Context): Value[Boolean] = find(value) <> null

  /**
   * Clears the contents of the collection.
   *
   * @param context Parse context.
   */
  def delete()(implicit context: Context): Unit = foreach { case (k, _) => set(k, Null) }

  /**
   * Returns whether or not the specified index is present in the collection.
   *
   * @param key Index.
   * @param context Parse context.
   * @return Whether or not the key is in the collection.
   */
  def exists(key: Value[K])(implicit context: Context): Value[Boolean] = {
    val x = Variable.Local[Boolean](context.label())
    x := False
    foreach { case (k, _) => x := x || k === key }
    x
  }

  /**
   * Returns the first key that corresponds to the specified value.
   *
   * @param value Value.
   * @param context Parse context.
   * @return Index of the value or [[Null]].
   */
  def find(value: Value[V])(implicit context: Context): Value[K] = {
    val index = Variable.Local[K](context.label())
    index := Null
    foreach { case (i, v) => If (index === Null && v === value)(index := i) }
    index
  }

  /**
   * Applies the specified function to each entry in the collection.
   *
   * @param f Function.
   * @param context Parse context.
   */
  def foreach[U](f: (Value[K], Value[V]) => U)(implicit context: Context): Unit

  /**
   * Returns the value at the specified index.
   *
   * @param key Index.
   * @param context Parse context.
   * @return Corresponding value.
   */
  def get(key: Value[K])(implicit context: Context): Value[V]

  /**
   * Updates the value at the specified index.
   *
   * @param key Index.
   * @param value Updated value.
   * @param context Parse context.
   */
  def set(key: Value[K], value: Value[V])(implicit context: Context): Unit

  /**
   * Returns the number of values in the collection.
   *
   * @return Length.
   */
  def size: Value[Int]

  /**
   * Removes the first occurrence of the specified value from the collection.
   *
   * @param value Removed value.
   * @param context Parse context.
   */
  def remove(value: Value[V])(implicit context: Context): Unit = {
    val key = find(value)
    If (key <> Null) { set(key, Null) }
  }

  /**
   * Returns whether or not this collection contains the same entries as the specified collection.
   *
   * @param that Collection.
   * @param context Parse context.
   * @return Whether or not the collections are equal.
   */
  def ===(that: Collection[K, V])(implicit context: Context): Value[Boolean] = {
    val x = Variable.Local[Boolean](context.label())
    x := this.size === that.size
    this foreach { case (k, _) => x := x && this(k) === that(k) }
    x
  }

}
