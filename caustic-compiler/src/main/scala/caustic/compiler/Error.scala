package caustic.compiler

import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.{ParserRuleContext, Token}

/**
 * A compiler exception.
 */
sealed trait Error extends ParseCancellationException {

  /**
   * Returns the unique error code associated with the exception.
   *
   * @return Error code.
   */
  def code: Int

  /**
   * Returns the title of the exception.
   *
   * @return Title name.
   */
  def title: String

  /**
   * Returns a detailed description of the error.
   *
   * @return Detailed summary.
   */
  def description: String

  /**
   * Returns the location of the error in the source file.
   *
   * @return Error location.
   */
  def trace: Error.Trace

}

object Error {

  /**
   * A source location.
   *
   * @param line Line number.
   * @param columns Range of offending columns.
   */
  case class Trace(line: Int, columns: Range)

  object Trace {

    /**
     * Constructs a trace from the specified ANTLR token.
     *
     * @param token ANTLR token.
     * @return Source location.
     */
    def apply(token: Token): Trace = {
      val length = token.getStopIndex - token.getStartIndex
      val initial = token.getCharPositionInLine
      Trace(token.getLine, initial to initial + length)
    }

    /**
     * Constructs a trace from the specified ANTLR rule.
     *
     * @param rule ANTLR rule.
     * @return Source location.
     */
    def apply(rule: ParserRuleContext): Trace = {
      if (rule.start.getLine != rule.stop.getLine) {
        Trace(rule.stop)
      } else {
        val length = rule.stop.getStopIndex - rule.start.getStartIndex
        val initial = rule.start.getCharPositionInLine
        Trace(rule.start.getLine, initial to initial + length)
      }
    }
  }

  /**
   * An error indicated unknown failure.
   *
   * @param description Detailed summary.
   * @param trace Source location.
   */
  case class Unknown(description: String, trace: Error.Trace) extends Error {
    override val code: Int = 0
    override val title: String = "Unknown Error"
  }

  /**
   * An error indicated the program is syntactically invalid.
   *
   * @param description Detailed summary.
   * @param trace Source location.
   */
  case class Syntax(description: String, trace: Error.Trace) extends Error {
    override val code: Int = 1
    override val title: String = "Syntax Error"
  }

  /**
   * An error indicating that a type does not exist or does not match its expected value.
   *
   * @param description Detailed summary.
   * @param trace Source location.
   */
  case class Type(description: String, trace: Error.Trace) extends Error {
    override val code: Int = 2
    override val title: String = "Type Error"
  }

  /**
   * An error indicating that the program could not be parsed.
   *
   * @param context ANTLR parsing context.
   */
  case class Parse(context: ParserRuleContext) extends Error {
    override val code: Int = 3
    override val title: String = "Parse Error"
    override val description: String = s"Unable to parse ${ context.getText }"
    override val trace: Trace = Error.Trace(context)
  }

}