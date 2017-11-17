package caustic.compiler

import org.antlr.v4.runtime._
import org.antlr.v4.runtime.tree.TerminalNode

import java.nio.file.Path
import scala.Console._
import scala.io.Source

/**
 * An exception handler.
 *
 * @param source Source file.
 * @param contents File contents.
 */
case class Handler(
  source: Path,
  contents: Seq[String]
) extends BaseErrorListener {

  // Number of errors detected.
  var errors: Int = 0

  // Number of warnings detected.
  var warnings: Int = 0

  /**
   * Reports the specified error.
   *
   * @param error Detected [[Error]].
   * @param message Error message.
   * @param line Line number.
   * @param columns Offending columns.
   */
  def report(error: Error, message: String, line: Int, columns: Range): Unit = {
    // Update the exception counters.
    error match {
      case _: Warning => this.warnings += 1
      case _: Error => this.errors += 1
    }

    // Display the error message.
    val heading = error match {
      case x: Warning => s"""$YELLOW[W${ "%03d".format(x.code) }] ${ x.title }"""
      case x: Error => s"""$RED[E${ "%03d".format(x.code) }] ${ x.title }"""
    }

    System.out.println(
      s"""$heading: ${ this.source.getParent }"/"${ this.source.getFileName } $RESET
         |$CYAN${ "-" * 100 }$RESET
         |$CYAN ${ "%4d".format(line) }  |$RESET ${ this.contents(line - 1) }
         |$CYAN       |$RESET ${ " " * columns.start }$CYAN${ "^" * columns.size }$RESET
         |$CYAN       |$RESET ${ message.grouped(91).mkString(s"\n|       $CYAN|$RESET ") }
       """.stripMargin
    )
  }

  /**
   * Reports the specified error.
   *
   * @param error Detected [[Error]].
   * @param message Error message.
   * @param token Offending ANTLR [[Token]].
   */
  def report(error: Error, message: String, token: Token): Unit = {
    val length = token.getStopIndex - token.getStartIndex
    val initial = token.getCharPositionInLine
    report(error, message, token.getLine, initial to initial + length)
  }

  /**
   * Reports the specified error.
   *
   * @param error Detected [[Error]].
   * @param message Error message.
   * @param rule Offending ANTLR [[ParserRuleContext]].
   */
  def report(error: Error, message: String, rule: ParserRuleContext): Unit = {
    if (rule.start.getLine != rule.stop.getLine) {
      report(error, message, rule.stop)
    } else {
      val length = rule.stop.getStopIndex - rule.start.getStartIndex
      val initial = rule.start.getCharPositionInLine
      report(error, message, rule.start.getLine, initial to initial + length)
    }
  }

  /**
   * Reports the specified error.
   *
   * @param error Detected [[Error]].
   * @param message Error message.
   * @param node Offending ANTLR [[TerminalNode]].
   */
  def report(error: Error, message: String, node: TerminalNode): Unit =
    report(error, message, node.getSymbol)

  override def syntaxError(
    recognizer: Recognizer[_, _],
    offendingSymbol: Any,
    line: Int,
    position: Int,
    message: String,
    exception: RecognitionException
  ): Unit = {
    // Convert an ANTLR RecognitionException into an Error.
    val error = exception match {
      case null => UnknownError
      case _ => SyntaxError
    }

    // Report the error.
    offendingSymbol match {
      case token: Token => report(error, message, token)
      case _ => report(error, message, line, position to position + 1)
    }
  }

}

object Handler {

  /**
   *
   * @param source
   * @return
   */
  def apply(source: Path): Handler =
    Handler(source, Source.fromFile(source.toFile).getLines().toSeq)

}