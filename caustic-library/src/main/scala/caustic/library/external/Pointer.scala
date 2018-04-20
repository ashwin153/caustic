package caustic.library.external

import spray.json._
import spray.json.DefaultJsonProtocol._

/**
 *
 * @param key
 * @tparam T
 */
case class Pointer[T](key: java.lang.String)

object Pointer {

  implicit def format[T]: JsonFormat[Pointer[T]] = new RootJsonFormat[Pointer[T]] {
    override def read(x: JsValue): Pointer[T] = Pointer(x.convertTo[String])
    override def write(x: Pointer[T]): JsValue = x.key.toJson
  }

}