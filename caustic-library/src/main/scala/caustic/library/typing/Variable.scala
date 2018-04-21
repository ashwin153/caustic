package caustic.library
package typing

import caustic.library.typing.Value._
import caustic.runtime._

/**
 * A scalar variable.
 */
sealed trait Variable[T <: Primitive] extends Value[T] {

  /**
   * Returns the name of the variable.
   *
   * @return Variable name.
   */
  def key: Value[String]

  /**
   * Updates the value of the variable.
   *
   * @param that Updated value.
   * @param context Parsing context.
   */
  def set(that: Value[T])(implicit context: Context): Unit

  /**
   * Scopes the variable with the specified namespace.
   *
   * @param x Namespace.
   * @return Scoped variable.
   */
  def scope[U <: Primitive](x: Value[String]): Variable[U] = this match {
    case Variable.Local(k) => Variable.Local(k + "/" + x)
    case Variable.Remote(k) => Variable.Remote(k + "/" + x)
  }

}

object Variable {

  /**
   * A local variable.
   *
   * @param key Variable name.
   */
  case class Local[T <: Primitive](key: Value[String]) extends Variable[T] {
    override def get: Program = load(key)
    override def set(that: Value[T])(implicit context: Context): Unit = context += store(key, that)
  }

  /**
   * A remote variable.
   *
   * @param key Variable name.
   */
  case class Remote[T <: Primitive](key: Value[String]) extends Variable[T] {
    override def get: Program = read(key)
    override def set(that: Value[T])(implicit context: Context): Unit = context += write(key, that)
  }

  // Implicit Operations.
  implicit class AssignmentOps[X <: Primitive](x: Variable[X]) {
    def :=[Y <: X](y: Value[Y])(implicit context: Context): Unit = x.set(y)
  }

  implicit class CompoundAssignment[X >: Int <: Double](x: Variable[X]) {
    def +=[Y <: X](y: Value[Y])(implicit context: Context): Unit = x := (x + y)
    def -=[Y <: X](y: Value[Y])(implicit context: Context): Unit = x := (x - y)
    def *=[Y <: X](y: Value[Y])(implicit context: Context): Unit = x := (x * y)
    def /=[Y <: X](y: Value[Y])(implicit context: Context): Unit = x := (x / y)
    def %=[Y <: X](y: Value[Y])(implicit context: Context): Unit = x := (x % y)
  }

}