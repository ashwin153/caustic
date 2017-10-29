package caustic.compiler

import caustic.grammar.{CausticLexer, CausticParser}
import caustic.compiler.core.{BlockGenerator, DeclarationGenerator}
import caustic.compiler.types._

import org.antlr.v4.runtime.{CharStream, CharStreams, CommonTokenStream}

import java.nio.file.Path

/**
 *
 */
object Compiler {

  /**
   *
   * @param args
   */
  def main(args: Array[String]): Unit = {
    compile(
      """
        | record Total {
        |   count: Int
        | }
        |
        | service Counter {
        |
        |   def inc(x: Total&): Int = {
        |     x.count += 1
        |   }
        |
        | }
      """.stripMargin
    )
  }

  /**
   *
   * @param stream
   * @return
   */
  def compile(stream: CharStream): String = {
    val lexer = new CausticLexer(stream)
    val tokens = new CommonTokenStream(lexer)
    val parser = new CausticParser(tokens)

    val universe = Universe.root
    DeclarationGenerator(universe).visitProgram(parser.program())
    println(universe)

    "x"
  }

  /**
   *
   * @param source
   * @return
   */
  def compile(source: String): String =
    compile(CharStreams.fromString(source))

  /**
   *
   * @param path
   * @return
   */
  def compile(path: Path): String =
    compile(CharStreams.fromPath(path))

}
