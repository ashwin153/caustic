package caustic.runtime.typing

import caustic.runtime._

/**
 *
 * @param body
 */
case class Context(
  private[lang] var body: Program = Null
) {

  /**
   *
   * @param that
   */
  def +=(that: Program): Unit =
    this.body = cons(this.body, that)

}