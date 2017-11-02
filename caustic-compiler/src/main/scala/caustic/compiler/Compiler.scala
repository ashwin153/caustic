package caustic.compiler

import caustic.compiler.Context.{SyntaxError, fail}
import caustic.compiler.types.Universe
import caustic.grammar.{CausticLexer, CausticParser}

import org.antlr.v4.runtime._

import java.nio.file.{Path, Paths}
import scala.io.Source
import scala.util.Try

/**
 * A compiler for the Caustic programming language.
 */
object Compiler {

  def main(args: Array[String]): Unit = {
    // Print usage information.
    if (args.length < 2) {
      println(
        """Usage: cc <goal> <file>
          |
          |Goals:
          |  - generate: Generates a Scala implementation.
          |  - declare: Evaluates declarations.
          |  - simplify: Simplifies expressions.
        """.stripMargin
      )

      System.exit(1)
    }

    // Run the compiler on the specified goal and path.
    args(0) match {
      case "generate" => compile(Paths.get(args(1)), goals.Generate)
      case "declare"  => compile(Paths.get(args(1)), goals.Declare(Universe.root))
      case "simplify" => compile(Paths.get(args(1)), goals.Simplify(Universe.root))
    }
  }

  /**
   * Returns the result of compiling the file with the specified goal.
   *
   * @param path Source file.
   * @param goal Compiler [[Goal]].
   * @return Result of performing goal on file.
   */
  def compile[T](path: Path, goal: Goal[T]): Try[T] = {
    Context.filename = path.getParent + "/" + path.getFileName
    Context.source = Source.fromFile(path.toFile).getLines().toSeq

    // Lex and parse the program.
    val lexer = new CausticLexer(CharStreams.fromPath(path))
    val tokens = new CommonTokenStream(lexer)
    val parser = new CausticParser(tokens)

    // Utilize the customized reporter for parse errors.
    parser.removeErrorListeners()
    parser.addErrorListener(new BaseErrorListener {
      override def syntaxError(
        recognizer: Recognizer[_, _],
        offendingSymbol: Any,
        line: Int,
        position: Int,
        message: String,
        exception: RecognitionException
      ): Unit = fail(SyntaxError, line, position to position + 1, message)
    })

    // Execute the goal on the parsed program.
    goal.execute(parser)
  }

}