package caustic.library.control

import caustic.library.typing._
import caustic.runtime

/**
 * A parsing context.
 *
 * @param body Current program.
 */
case class Context(
  private[library] var body: runtime.Program = Null,
  private[library] var current: scala.Int = -1
) {

  /**
   * Appends the program to the context.
   *
   * @param that Program to append.
   */
  def +=(that: runtime.Program): Unit =
    this.body = runtime.cons(this.body, that)

  /**
   *
   * @return
   */
  def label(): Value[String] = {
    this.current += 1
    string("$") ++ this.current
  }

}