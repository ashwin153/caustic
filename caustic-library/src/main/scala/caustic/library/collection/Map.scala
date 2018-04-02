package caustic.library.collection

import caustic.library.control._
import caustic.library.typing._
import caustic.runtime.Null

/**
 * A dynamically-sized collection of key-value pairs.
 *
 * @param size Number of keys.
 * @param keys Current keys.
 */
case class Map[T <: Primitive](
  size: Variable[Int],
  keys: Set[String]
) {

  /**
   * Returns the value of the specified key.
   *
   * @param key Key to lookup.
   * @return Current value.
   */
  def get(key: Value[String]): Value[T] = this.size.scope(key)

  /**
   * Updates the value of the specified key.
   *
   * @param key Key to update.
   * @param value Updated value.
   * @param context Parse context.
   */
  def set(key: Value[String], value: Value[T])(implicit context: Context): Unit = {
    If (value === Null) {
      this.keys.remove(key)
    } Else {
      this.keys.add(key)
    }

    this.size.scope[T](key) := value
  }

  /**
   * Applies the function to each key-value pair.
   *
   * @param f Function to apply.
   * @param context Parse context.
   */
  def foreach[U](f: (Value[String], Value[T]) => U)(implicit context: Context): Unit = {
    this.keys.foreach(k => f(k, get(k)))
  }

  /**
   * Removes all key-value pairs.
   *
   * @param context Parse context.
   */
  def clear()(implicit context: Context): Unit = foreach { case (k, _) => set(k, Null) }

}
