package caustic.runtime
package service

import java.io.Closeable
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
 * A Caustic, Thrift client.
 */
trait Client extends Closeable {

  /**
   * Attempts to execute the transaction and automatically retries failures with backoff.
   *
   * @param transaction Transaction to execute.
   * @param backoffs Backoff durations.
   * @return Execution result or an error on failure.
   */
  def execute(transaction: thrift.Transaction, backoffs: Seq[FiniteDuration]): Try[thrift.Literal]

  /**
   * Attempts to execute the transaction.
   *
   * @param transaction Transaction to execute.
   * @return Execution result or an error on failure.
   */
  def execute(transaction: thrift.Transaction): Try[thrift.Literal] =
    execute(transaction, Seq.empty)

}
