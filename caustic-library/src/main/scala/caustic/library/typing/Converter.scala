package caustic.library.typing

/**
 *
 * @tparam A
 * @tparam B
 */
trait Converter[A, B <: Primitive] {

  /**
   *
   * @param x
   * @return
   */
  def apply(x: A): Value[B]

}

object Converter {

  implicit val string: Converter[java.lang.String, String] = x => x
  implicit val double: Converter[scala.Double, Double] = x => x
  implicit val long: Converter[scala.Long, Int] = x => x
  implicit val int: Converter[scala.Int, Int] = x => x
  implicit val boolean: Converter[scala.Boolean, Boolean] = x => x

}