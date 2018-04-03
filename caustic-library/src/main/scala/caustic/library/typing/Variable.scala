package caustic.library.typing

import caustic.library.control.Context
import caustic.runtime._

/**
 * A scalar variable.
 */
trait Variable[T <: Primitive] extends Value[T] {

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
    case Local(a) => Local[U](this.key ++ "@@" ++ x)
    case Remote(a) => Remote[U](this.key ++ "@@" ++ x)
  }

}

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