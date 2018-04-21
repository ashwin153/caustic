package caustic.library
package typing
package collection

import caustic.library.control._
import caustic.library.typing.Value._
import caustic.runtime.{Null, prefetch}

import scala.language.reflectiveCalls

/**
 * A mutable collection of key-value pairs.
 *
 * @param length
 */
class Map[K <: String, V <: Primitive](length: Variable[Int]) extends Collection[K, V] {

  /**
   *
   */
  val keys: Set[K] = new Set(length)

  override def size: Value[Int] = this.keys.size

  override def get(key: Value[K])(implicit context: Context): Value[V] =
    this.length.scope(this.keys.find(key)).scope(key)

  override def set(key: Value[K], value: Value[V])(implicit context: Context): Unit = {
    If (value === Null) {
      this.length.scope(this.keys.find(key)).scope(key) := Null
      this.keys.remove(key)
    } Else {
      this.keys.add(key)
      this.length.scope(this.keys.find(key)).scope[V](key) := value
    }
  }

  override def foreach[U](f: (Value[K], Value[V]) => U)(implicit context: Context): Unit = {
    // Prefetch all key-value pairs if the map is remotely stored.
    if (this.length.isInstanceOf[Variable.Remote[Int]])
      context += prefetch(this.length.key, this.size, 2)

    // Iterate over each key-value pair in the map.
    this.keys foreach { case (_, k) => f(k, this(k)) }
  }

  override def asJson(implicit context: Context): Value[String] = {
    val json = Variable.Local[String](context.label())
    json := "{"

    foreach { case (k, v) =>
      If (json === "{") {
        json := json + k.quoted + ": " + v.asJson
      } Else {
        json := json + ", " + k.quoted + ": " + v.asJson
      }
    }

    json + "}"
  }

}

object Map {

  /**
   *
   * @param key
   * @return
   */
  def apply[K <: String, V <: Primitive](key: Variable[Int]): Map[K, V] = new Map(key)

  /**
   *
   * @param key
   * @return
   */
  def Local[K <: String, V <: Primitive](key: Value[String]): Map[K, V] = Map(Variable.Local(key))

  /**
   *
   * @param key
   * @return
   */
  def Remote[K <: String, V <: Primitive](key: Value[String]): Map[K, V] = Map(Variable.Remote(key))

  // Implicit Operations.
  implicit class AssignmentOps[K <: String, V <: Primitive](x: Map[K, V]) {
    def :=(y: Map[K, V])(implicit context: Context): Unit = { x.delete(); x ++= y }
  }

  implicit class CompoundAssignmentOps[K <: String, V <: Primitive](x: Map[K, V]) {
    def ++=(y: Map[K, V])(implicit context: Context): Unit = y.foreach(x.set)
    def --=(y: Map[K, V])(implicit context: Context): Unit = y foreach { case (k, _) => x -= k }
    def +=(kv: (Value[K], Value[V]))(implicit context: Context): Unit = x.set(kv._1, kv._2)
    def -=(k: Value[K])(implicit context: Context): Unit = x.set(k, Null)
  }

}