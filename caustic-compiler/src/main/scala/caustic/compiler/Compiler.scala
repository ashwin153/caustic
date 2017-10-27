package caustic.compiler

import caustic.grammar.{CausticLexer, CausticParser}
import caustic.compiler.generate.{BlockGenerator, DeclarationGenerator}
import caustic.compiler.typing._

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
    println(compile(
      """ {
        |   something.foo = 3
        | }
      """.stripMargin
    ))
//    println(compile(
//      """ {
//        |   var x = 3
//        |   var y = "hello"
//        |
//        |   if (x < y) {
//        |     var z = 5
//        |     x = 4
//        |   } else {
//        |     y = 2
//        |   }
//        | }
//      """.stripMargin
//    ))
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
    BlockGenerator(Universe.root).visitBlock(parser.block())
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
