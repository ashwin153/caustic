package caustic.library.typing

import spray.json._
import DefaultJsonProtocol._

/**
 *
 * @param key
 */
case class Pointer[T](key: java.lang.String)

object Pointer {

  // Implicit Conversions.
  implicit def format[T]: JsonFormat[Pointer[T]] = new JsonFormat[Pointer[T]] {
    override def write(x: Pointer[T]): JsValue = x.key.toJson
    override def read(x: JsValue): Pointer[T] = Pointer(x.convertTo[java.lang.String])
  }

}