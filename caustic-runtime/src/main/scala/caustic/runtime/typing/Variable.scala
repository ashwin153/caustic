package caustic.runtime.typing

import caustic.runtime._

/**
 *
 * @tparam T
 */
trait Variable[T <: Primitive] extends Value[T] {

  /**
   *
   * @return
   */
  def key: Value[String]

  /**
   *
   * @param that
   */
  def set(that: Value[T])(implicit context: Context): Unit

}

object Variable {

  /**
   *
   * @param x
   * @tparam T
   */
  implicit class Assignment[T <: Primitive](x: Variable[T]) {
    def :=(y: Value[T])(implicit context: Context): Unit = x.set(y)
  }

  /**
   *
   * @param x
   * @tparam T
   */
  implicit class CompoundAssignment[T <: Double](x: Variable[T]) {
    def +=(y: Value[T])(implicit context: Context): Unit = x := (x + y)
    def -=(y: Value[T])(implicit context: Context): Unit = x := (x - y)
    def *=(y: Value[T])(implicit context: Context): Unit = x := (x * y)
    def /=(y: Value[T])(implicit context: Context): Unit = x := (x / y)
  }

  /**
   *
   * @param x
   * @tparam T
   */
  implicit class Scope[T <: Primitive](x: Variable[T]) {
    def scope[U](b: Value[String]): Variable[U] = x match {
      case Variable.Local(a) => Variable.Local[U](a ++ "@@" ++ b)
      case Variable.Remote(a) => Variable.Remote[U](a ++ "@@" ++ b)
    }
  }

  /**
   *
   * @param key
   * @tparam T
   */
  case class Local[T <: Primitive](key: Value[String]) extends Variable[T] {

    override def get: Program = load(key)

    override def set(that: Value[T])(implicit context: Context): Unit = context += store(key, that)

  }

  /**
   *
   * @param key
   * @tparam T
   */
  case class Remote[T <: Primitive](key: Value[String]) extends Variable[T] {

    override def get: Program = read(key)

    override def set(that: Value[T])(implicit context: Context): Unit = context += write(key, that)

  }

}