package caustic.compiler

import org.antlr.v4.runtime._
import org.antlr.v4.runtime.atn.ATNSimulator

import java.nio.file.Path
import scala.Console._

/**
 * An exception handler.
 *
 * @param source Source file.
 * @param contents File contents.
 */
case class Handler(source: Path, contents: Seq[String]) extends BaseErrorListener {

  override def syntaxError(
    recognizer: Recognizer[_, _],
    offendingSymbol: Any,
    line: Int,
    position: Int,
    message: String,
    exception: RecognitionException
  ): Unit = {
    // Determine the location of the exception.
    val trace = offendingSymbol match {
      case token: Token => Error.Trace(token)
      case _ => Error.Trace(line, position to position + 1)
    }

    // Convert an ANTLR RecognitionException into an Error.
    exception match {
      case null => report(Error.Unknown(message, trace))
      case _ => report(Error.Syntax(message, trace))
    }
  }

  /**
   *
   * @return
   */
  def file: String = s"""${ this.source.getParent }"/"${ this.source.getFileName }"""

  /**
   * Reports the specified error.
   *
   * @param error Detected Error.
   */
  def report(error: Error): Unit = {
    // Display the error message to the console.
    System.out.println(
      s"""$RED[E${ "%03d".format(error.code) }] ${ error.title }: $file $RESET
         |$CYAN${ "-" * 100 }$RESET
         |$CYAN ${ "%4d".format(error.trace.line) }  |$RESET ${ this.contents(error.trace.line - 1) }
         |$CYAN       |$RESET ${ " " * error.trace.columns.start }$CYAN${ "^" * error.trace.columns.size }$RESET
         |$CYAN       |$RESET ${ error.description.grouped(91).mkString(s"\n|       $CYAN|$RESET ") }
       """.stripMargin
    )
  }

}