package caustic.compiler

import caustic.grammar._
import java.nio.file.Path
import org.antlr.v4.runtime.{CharStream, CharStreams, CommonTokenStream}

/**
 *
 */
trait Query[T] {

  /**
   *
   * @param parser
   * @return
   */
  def execute(parser: CausticParser): T

  /**
   *
   * @param stream
   * @return
   */
  def execute(stream: CharStream): T = {
    val lexer = new CausticLexer(stream)
    val tokens = new CommonTokenStream(lexer)
    execute(new CausticParser(tokens))
  }

  /**
   *
   * @param source
   * @return
   */
  def execute(source: String): T =
    execute(CharStreams.fromString(source))

  /**
   *
   * @param path
   * @return
   */
  def execute(path: Path): T =
    execute(CharStreams.fromPath(path))

}
