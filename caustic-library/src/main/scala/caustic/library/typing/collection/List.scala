package caustic.library.typing
package collection

import caustic.library.Context
import caustic.library.control._
import caustic.library.typing.Value._

import caustic.runtime._
import scala.language.reflectiveCalls

/**
 * A mutable collection of values.
 *
 * @param length Current size.
 */
class List[T <: Primitive](length: Variable[Int]) extends Collection[Int, T] {

  override def size: Value[Int] = this.length

  override def get(key: Value[Int])(implicit context: Context): Value[T] =
    this.length.scope[T](key)

  override def set(key: Value[Int], value: Value[T])(implicit context: Context): Unit = {
    If (exists(key) && value === Null) {
      // Shift over all values after the index of the removed value.
      foreach { case (i, _) =>
        If (i >= key && i < this.length - 1) {
          this.length.scope[T](i) := get(i + 1)
        }
      }

      // Decrement the size of the list.
      this.length.scope[T](this.length - 1) := Null
      this.length -= 1
    } Else {
      // Update the value of the key.
      this.length.scope[T](key) := value
      this.length := (this.length max key) + 1
    }
  }

  override def foreach[U](f: (Value[Int], Value[T]) => U)(implicit context: Context): Unit = {
    // Prefetch the list if it is remotely stored.
    this.length match {
      case _: Variable.Remote[Int] => context += prefetch(this.length.key, this.length, 1)
      case _ =>
    }

    // Iterate through each item in the list.
    val index = Variable.Local[Int](context.label())
    index := 0

    While (index < this.length) {
      f(index, this(index))
    }
  }

  override def asJson(implicit context: Context): Value[String] = {
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

}

object List {

  /**
   *
   * @param key
   * @tparam T
   * @return
   */
  def apply[T <: Primitive](key: Variable[Int]): List[T] = new List[T](key)

  /**
   *
   * @param key
   * @return
   */
  def Local[T <: Primitive](key: Value[String]): List[T] = List(Variable.Local(key))

  /**
   *
   * @param key
   * @return
   */
  def Remote[T <: Primitive](key: Value[String]): List[T] = List(Variable.Remote(key))

  // Implicit Operations.
  implicit class AssignmentOps[T <: Primitive](x: List[T]) {
    def :=(y: List[T])(implicit context: Context): Unit = {
      x.delete()
      x ++= y
    }
  }

  implicit class CompoundAssignmentOps[T <: Primitive](x: List[T]) {
    def ++=(y: List[T])(implicit context: Context): Unit = y foreach { case (_, v) => x += _ }
    def --=(y: List[T])(implicit context: Context): Unit = y foreach { case (_, v) => x -= _ }
    def +=(y: Value[T])(implicit context: Context): Unit = x.set(x.size, y)
    def -=(y: Value[T])(implicit context: Context): Unit = x.remove(y)
  }

}