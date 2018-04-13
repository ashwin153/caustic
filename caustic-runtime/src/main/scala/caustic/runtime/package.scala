package caustic

import scala.language.implicitConversions

package object runtime extends Builder {

  // Implicit Conversions.
  implicit def bol2flag(value: Boolean): Flag = if (value) True else False
  implicit def num2real[T](value: T)(implicit num: Numeric[T]): Real = Real(num.toDouble(value))
  implicit def str2text(value: String): Text = if (value.isEmpty) Empty else Text(value)

}