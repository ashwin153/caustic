package caustic.compiler

import caustic.grammar._
import org.antlr.v4.runtime.{CharStream, CharStreams, CommonTokenStream}
import java.nio.file.Path
import scala.util.Try

/**
 * A compiler goal.
 */
trait Goal[T] {

  /**
   *
   * @param parser
   * @return
   */
  def execute(parser: CausticParser): Try[T]

  /**
   *
   * @param stream
   * @return
   */
  final def execute(stream: CharStream): Try[T] = {
    // Construct a parser for the source file.
    val lexer = new CausticLexer(stream)
    val tokens = new CommonTokenStream(lexer)
    val parser = new CausticParser(tokens)
//    val reporter = Reporter(path, Source.fromFile(path.toFile).getLines().toSeq)
//
//    // Override the default ANTLR error handler.
//    parser.removeErrorListeners()
//    parser.addErrorListener(reporter)

    // Execute the query.
    execute(parser)
  }

  /**
   *
   * @param source
   * @return
   */
  final def execute(source: String): Try[T] =
    execute(CharStreams.fromString(source))

  /**
   *
   * @param path
   * @return
   */
  final def execute(path: Path): Try[T] =
    execute(CharStreams.fromPath(path))

}
