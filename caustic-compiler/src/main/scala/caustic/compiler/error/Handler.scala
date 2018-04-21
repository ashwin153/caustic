package caustic.compiler.error

import org.antlr.v4.runtime._

import java.io.PrintStream
import scala.Console._

/**
 * An exception handler.
 *
 * @param file Source file.
 * @param source Source code.
 * @param output Output stream.
 */
case class Handler(
  file: String,
  source: Seq[String],
  output: PrintStream = System.out
) extends BaseErrorListener {

  /**
   * Prints the specified error to the output stream. Error messages are formatted for display in a
   * 100 character bash shell. Stylistically inspired by the Dotty compiler.
   *
   * @see https://scala-lang.org/blog/2016/10/14/dotty-errors.html
   * @param error Detected Error.
   */
  def report(error: Error): Unit = this.output.println {
    s"""$RED[E${ "%03d".format(error.code) }] ${ error.title }: $file $RESET
       |$CYAN${ "-" * 100 }$RESET
       |$CYAN ${ "%4d".format(error.trace.line) }  |$RESET ${ this.source(error.trace.line - 1) }
       |$CYAN       |$RESET ${ " " * error.trace.columns.start }$CYAN${ "^" * error.trace.columns.size }$RESET
       |$CYAN       |$RESET ${ error.description.grouped(91).mkString(s"\n|       $CYAN|$RESET ") }
     """.stripMargin
  }

  override def syntaxError(
    recognizer: Recognizer[_, _],
    offendingSymbol: Any,
    line: Int,
    position: Int,
    message: String,
    exception: RecognitionException
  ): Unit = {
    val trace = offendingSymbol match {
      case token: Token => Trace(token)
      case _ => Trace(line, position to position + 1)
    }

    exception match {
      case null => report(Error.Unknown(message, trace))
      case _ => report(Error.Syntax(message, trace))
    }
  }

}