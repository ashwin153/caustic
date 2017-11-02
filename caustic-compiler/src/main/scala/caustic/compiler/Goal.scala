package caustic.compiler

import caustic.grammar.CausticParser

import scala.util.Try

/**
 * A compiler goal.
 */
trait Goal[T] {

  /**
   * Executes the goal on the parsed abstract-syntax tree.
   *
   * @param parser ANTLR parser.
   * @return Result of executing goal.
   */
  def execute(parser: CausticParser): Try[T]


}
