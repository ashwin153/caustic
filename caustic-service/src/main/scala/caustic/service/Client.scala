package caustic.service

import caustic.runtime.thrift

import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.{TFramedTransport, TSocket}

import java.io.Closeable
import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
 * A Thrift connection for server instances. Not thread-safe.
 *
 * @param host Instance hostname.
 * @param port Instance port number.
 */
case class Client(
  host: String,
  port: Int
) extends Closeable {

  // Setup the Thrift transport.
  val transport = new TFramedTransport(new TSocket(this.host, this.port))
  val protocol = new TBinaryProtocol(transport)
  val underlying = new thrift.Database.Client(protocol)
  this.transport.open()

  // Close the Thrift transport.
  override def close(): Unit = this.transport.close()

  /**
   * Returns the result of executing the transaction, and automatically retries failures.
   *
   * @param transaction Transaction to execute.
   * @param backoffs Backoff durations.
   * @return Execution result or an error on failure.
   */
  def execute(transaction: thrift.Transaction, backoffs: Seq[FiniteDuration]): Try[thrift.Literal] =
    Try(this.underlying.execute(transaction, backoffs.map(_.toMillis).map(long2Long).asJava))

  /**
   * Attempts to execute the transaction.
   *
   * @param transaction Transaction to execute.
   * @return Execution result or an error on failure.
   */
  def execute(transaction: thrift.Transaction): Try[thrift.Literal] =
    execute(transaction, Seq.empty)

}
