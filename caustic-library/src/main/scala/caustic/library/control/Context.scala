package caustic.library.control

import caustic.runtime._

/**
 * A parsing context.
 *
 * @param body Current program.
 */
case class Context(
  private[library] var body: Program = Null
) {

  /**
   * Appends the program to the context.
   *
   * @param that Program to append.
   */
  def +=(that: Program): Unit =
    this.body = cons(this.body, that)

}