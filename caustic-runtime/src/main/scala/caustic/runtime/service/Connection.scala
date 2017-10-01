package caustic.runtime
package service

import java.io.Closeable

import scala.util.Try

/**
 * A Caustic connection.
 */
trait Connection extends Closeable {

  /**
   * Attempts to execute the transaction and returns the result.
   *
   * @param transaction Transaction to execute.
   * @return Result of execution.
   */
  def execute(transaction: thrift.Transaction): Try[thrift.Literal]

}
