package caustic.syntax

import caustic.grammar._
import java.io.InputStream
import org.antlr.v4.runtime._
import scala.io.Source

/**
 *
 */
trait Compiler extends CausticBaseVisitor[String] {

  /**
   *
   * @param caustic Caustic source code.
   * @return
   */
  def compile(caustic: CharStream): String = {
    val lexer = new CausticLexer(caustic)
    val tokens = new CommonTokenStream(lexer)
    val parser = new CausticParser(tokens)
    this.visitService(parser.service())
  }

  final def compile(code: String): String =
    compile(CharStreams.fromString(code))

  final def compile(stream: InputStream): String =
    compile(CharStreams.fromStream(stream))

  final def compile(source: Source): String =
    try compile(source.mkString) finally source.close()

}
