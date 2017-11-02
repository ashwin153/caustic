package caustic.compiler

import org.antlr.v4.runtime._
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.tree.TerminalNode

import java.io.PrintStream
import scala.Console._

/**
 * A singleton exception handler.
 */
object Context extends BaseErrorListener {

  //Name of file under compilation.
  var filename: String = ""

  // Contents of file under compilation.
  var source: Seq[String] = Seq.empty

  // Output location for messages.
  var output: PrintStream = System.out

  /**
   * A fatal compiler error.
   *
   * @param code Error code.
   * @param title Name.
   */
  sealed abstract class Error(val code: Int, val title: String)
  case object SyntaxError extends Error(0, "Syntax Error")
  case object TypeError extends Error(1, "Type Error")

  /**
   * A non-fatal compiler warning.
   *
   * @param code Warning code.
   * @param title Name.
   */
  sealed abstract class Warning(val code: Int, val title: String)
  case object Shadowing extends Warning(0, "Name Shadowing")

  /**
   * Prints an error message to the output stream and terminates compiler.
   *
   * @param error Failure.
   * @param line Line number.
   * @param columns Offending columns.
   * @param message Description.
   */
  def fail(error: Error, line: Int, columns: Range, message: String): Unit = {
    // Copied from Dotty. https://scala-lang.org/blog/2016/10/14/dotty-errors.html
    this.output.println(
      s"""$RED[E${ "%03d".format(error.code) }] ${ error.title }: $filename $RESET
         |$CYAN${ "-" * 100 }$RESET
         |$CYAN ${ "%4d".format(line) }  |$RESET ${ this.source(line - 1) }
         |$CYAN       |$RESET ${ " " * columns.start }$CYAN${ "^" * columns.size }$RESET
         |$CYAN       |$RESET ${ message.grouped(91).mkString(s"\n|       $CYAN|$RESET ") }
       """.stripMargin
    )

    // Stop ANTLR on errors.
    throw new ParseCancellationException(message)
  }

  /**
   * Prints an error message to the output stream and terminates compiler.
   *
   * @param error Failure.
   * @param node Offending [[Token]].
   * @param message Description.
   */
  def fail(error: Error, node: TerminalNode, message: String): Unit = {
    val beg = node.getSymbol.getCharPositionInLine
    val end = beg + node.getSymbol.getStopIndex - node.getSymbol.getStopIndex
    fail(error, node.getSymbol.getLine, beg to end, message)
  }

  /**
   * Prints an warning message to the output stream.
   *
   * @param warning Warning.
   * @param line Line number.
   * @param columns Offending columns.
   * @param message Description.
   */
  def warn(warning: Warning, line: Int, columns: Range, message: String): Unit =
    // Copied from Dotty. https://scala-lang.org/blog/2016/10/14/dotty-errors.html
    this.output.println(
      s"""$RED[E${ "%03d".format(warning.code) }] ${ warning.title }: $filename $RESET
         |$CYAN${ "-" * 100 }$RESET
         |$CYAN ${ "%4d".format(line) }  |$RESET ${ this.source(line - 1) }
         |$CYAN       |$RESET ${ " " * columns.start }$CYAN${ "^" * columns.size }$RESET
         |$CYAN       |$RESET ${ message.grouped(91).mkString(s"\n|       $CYAN|$RESET ") }
       """.stripMargin
    )

  /**
   * Prints an warn message to the output stream.
   *
   * @param warning Warning.
   * @param node Offending [[Token]].
   * @param message Description.
   */
  def warn(warning: Warning, node: TerminalNode, message: String): Unit = {
    val beg = node.getSymbol.getCharPositionInLine
    val end = beg + node.getSymbol.getStopIndex - node.getSymbol.getStopIndex
    warn(warning, node.getSymbol.getLine, beg to end, message)
  }

}