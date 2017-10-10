package caustic.service

import caustic.runtime.thrift

import java.io.Closeable
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
 * A Thrift client.
 */
trait Client extends Closeable {

  /**
   * Returns the result of executing the transaction, and automatically retries failures.
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
