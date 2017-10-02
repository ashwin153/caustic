package caustic.runtime
package service

import java.io.Closeable

import scala.util.Try

/**
 * A Caustic client.
 */
trait Client extends Closeable {

  /**
   * Attempts to execute the transaction and returns the result.
   *
   * @param transaction Transaction to execute.
   * @return Result of execution.
   */
  def execute(transaction: thrift.Transaction): Try[thrift.Literal]

}
