package caustic.runtime.collection

import caustic.lang.Value
import caustic.runtime.typing.{Boolean, Constant, Primitive, Value, Variable}
import scala.language.implicitConversions

/**
 *
 * @tparam T
 */
trait Constructor[T] {

  /**
   *
   * @param key
   * @return
   */
  def construct(key: Value[_]): T

}

object Constructor {

  /**
   *
   * @tparam T
   * @return
   */
  implicit def valueConstructor[T <: Primitive]: Constructor[Value[T]] =
    k => Constant[T](k.get)

  /**
   *
   * @tparam T
   * @return
   */
  implicit def variableConstructor[T <: Primitive]: Constructor[Variable[T]] =
    k => k.asInstanceOf[Variable[T]]

  /**
   *
   * @tparam T
   * @return
   */
  implicit def objectConstructor[T]: Constructor[Object[T]] =
    k => Object(k.asInstanceOf[Variable[Boolean]])

  /**
   *
   * @tparam T
   * @return
   */
  implicit def collectionConstructor[T]: Constructor[Collection[T]] =
    k => Collection(k.asInstanceOf[Variable[Int]])

}