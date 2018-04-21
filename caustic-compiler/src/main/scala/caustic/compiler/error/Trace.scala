package caustic.compiler.error

import org.antlr.v4.runtime.{ParserRuleContext, Token}

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
