package caustic.compiler.error

import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.ParserRuleContext

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
  def trace: Trace

}

object Error {

  /**
   * An error indicated unknown failure.
   *
   * @param description Detailed summary.
   * @param trace Source location.
   */
  case class Unknown(description: String, trace: Trace) extends Error {
    override val code: Int = 0
    override val title: String = "Unknown Error"
  }

  /**
   * An error indicated the program is syntactically invalid.
   *
   * @param description Detailed summary.
   * @param trace Source location.
   */
  case class Syntax(description: String, trace: Trace) extends Error {
    override val code: Int = 1
    override val title: String = "Syntax Error"
  }

  /**
   * An error indicating that a type does not exist or does not match its expected value.
   *
   * @param description Detailed summary.
   * @param trace Source location.
   */
  case class Type(description: String, trace: Trace) extends Error {
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
    override val trace: Trace = Trace(context)
  }

}