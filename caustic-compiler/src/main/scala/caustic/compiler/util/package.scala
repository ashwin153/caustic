package caustic.compiler

import scala.language.implicitConversions

package object util {

  // Implicit Conversions.
  implicit def indenter(context: StringContext): Indenter = new Indenter(context)

}
