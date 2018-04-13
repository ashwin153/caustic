package caustic.compiler

import scala.reflect.runtime.universe._
import scala.reflect.runtime._
import scala.tools.reflect.ToolBox
import scala.util.Try

/**
 * A compiler for the Scala programming language.
 */
object Scalac {

  // Scala Compiler.
  val toolbox: ToolBox[universe.type] = currentMirror.mkToolBox()

  /**
   * Attempts to compile the specified source code.
   *
   * @param source Source code.
   * @return Whether or not compilation was successful.
   */
  def compile(source: String): Try[Unit] = Try(this.toolbox.eval(this.toolbox.parse(source)))

  /**
   * Pretty prints the specified source code.
   *
   * @param source Source code.
   * @return Pretty printed code.
   */
  def format(source: String): String = showCode(this.toolbox.parse(source))

}
