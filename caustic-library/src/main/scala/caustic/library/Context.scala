package caustic.library

import caustic.library.typing._
import caustic.runtime._

/**
 * A parsing context.
 *
 * @param body Current program.
 */
case class Context(
  private[library] var body: Program = Null,
  private[library] var current: scala.Int = -1
) {

  /**
   * Appends the program to the context.
   *
   * @param that Program to append.
   */
  def +=(that: Program): Unit =
    this.body = cons(this.body, that)

  /**
   *
   * @return
   */
  def label(): Value[String] = {
    this.current += 1
    "$" + this.current
  }

}