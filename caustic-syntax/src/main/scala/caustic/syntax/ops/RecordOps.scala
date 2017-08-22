package caustic.syntax
package ops

import scala.language.dynamics

/**
 *
 * @param x
 */
case class RecordOps(x: Record) {

  /**
   *
   * @return
   */
  def exists: Transaction =
    read(x) <> Empty

}
