package caustic.library

import caustic.library.typing.Variable
import scala.language.implicitConversions

package object record {

  type Pointer[T] = java.lang.String

  // Implicit Conversions.
  implicit def reference[T, U](x: Pointer[T]): Reference[U] = Reference(Variable.Remote(x))

}
