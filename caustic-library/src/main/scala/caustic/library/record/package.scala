package caustic.library

import caustic.library.control.Context
import caustic.library.typing._

package object record {

  // Implicit Operations.
  implicit class Assignment[T](x: Reference[T]) {
    def :=(y: Reference[T])(implicit context: Context): Unit = x.pointer.set(y.key)
    def :=(y: Value[String])(implicit context: Context): Unit = x.pointer.set(y)
  }

}
