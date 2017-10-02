package caustic.runtime
package service

import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.{TFramedTransport, TSocket, TTransport}

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
 * A Thrift connection for single instances. Not thread-safe.
 *
 * @param transport Thrift transport.
 * @param underlying Underlying Thrift client.
 */
case class Connection(
  transport: TTransport,
  underlying: thrift.Database.Client
) extends Client {

  // Setup the Thrift transport.
  this.transport.open()

  override def close(): Unit =
    this.transport.close()

  override def execute(
    transaction: thrift.Transaction,
    backoffs: Seq[FiniteDuration]
  ): Try[thrift.Literal] =
    Try(this.underlying.execute(transaction, backoffs.map(_.toMillis).map(long2Long).asJava))

}

object Connection {

  /**
   * Constructs a connection to the specified instance.
   *
   * @param instance Server instance.
   * @return Instance connection.
   */
  def apply(instance: Instance): Connection = {
    val transport = new TFramedTransport(new TSocket(instance.host, instance.port))
    val protocol = new TBinaryProtocol(transport)
    val underlying = new thrift.Database.Client(protocol)
    Connection(transport, underlying)
  }

  /**
   * Constructs a connection to the specified hostname and port.
   *
   * @param host Hostname.
   * @param port Port number.
   * @return Instance connection.
   */
  def apply(host: String, port: Int): Connection =
    Connection(Instance(host, port))

}