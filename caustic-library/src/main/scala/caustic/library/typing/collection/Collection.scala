package caustic.library.typing
package collection

import caustic.library.Context
import caustic.library.control._

import caustic.runtime.Null

/**
 *
 */
trait Collection[K <: Primitive, V <: Primitive] extends Internal {

  /**
   *
   * @param key
   * @param context
   * @return
   */
  def apply(key: Value[K])(implicit context: Context): Value[V] = get(key)

  /**
   *
   * @param context
   * @return
   */
  def asJson(implicit context: Context): Value[String]

  /**
   *
   * @param value
   * @param context
   * @return
   */
  def contains(value: Value[V])(implicit context: Context): Value[Boolean] = find(value) <> null

  /**
   *
   * @param context
   */
  def delete()(implicit context: Context): Unit = foreach { case (k, _) => set(k, Null) }

  /**
   *
   * @param key
   * @param context
   * @return
   */
  def exists(key: Value[K])(implicit context: Context): Value[Boolean] = {
    val x = Variable.Local[Boolean](context.label())
    x := False
    foreach { case (k, _) => x := x || k === key }
    x
  }

  /**
   *
   * @param value
   * @param context
   */
  def find(value: Value[V])(implicit context: Context): Value[K] = {
    val index = Variable.Local[K](context.label())
    index := Null
    foreach { case (i, v) => If (index === Null && v === value)(index := i) }
    index
  }

  /**
   *
   * @param f
   * @param context
   */
  def foreach[U](f: (Value[K], Value[V]) => U)(implicit context: Context): Unit

  /**
   *
   * @param key
   * @param context
   * @return
   */
  def get(key: Value[K])(implicit context: Context): Value[V]

  /**
   *
   * @param key
   * @param value
   * @param context
   */
  def set(key: Value[K], value: Value[V])(implicit context: Context): Unit

  /**
   *
   * @return
   */
  def size: Value[Int]

  /**
   *
   * @param value
   * @param context
   */
  def remove(value: Value[V])(implicit context: Context): Unit = {
    val key = find(value)
    If (key <> Null) { set(key, Null) }
  }

  /**
   *
   * @param that
   * @param context
   * @return
   */
  def ===(that: Collection[K, V])(implicit context: Context): Value[Boolean] = {
    val x = Variable.Local[Boolean](context.label())
    x := this.size === that.size
    this foreach { case (k, _) => x := x && this(k) === that(k) }
    x
  }

}
