package caustic.runtime
package service

import java.io.Closeable

import scala.util.Try

/**
 *
 */
trait Connection extends Closeable {

  /**
   *
   * @param transaction
   * @return
   */
  def execute(transaction: thrift.Transaction): Try[thrift.Literal]

}
