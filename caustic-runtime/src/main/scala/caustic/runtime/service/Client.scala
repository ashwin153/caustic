package caustic.runtime
package service

import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.{TSocket, TTransport}

import scala.util.Try

/**
 * A Thrift connection for single instances.
 *
 * @param transport Thrift transport.
 * @param underlying Underlying Thrift client.
 */
case class Client(
  transport: TTransport,
  underlying: thrift.Database.Client
) extends Connection {

  // Setup the Thrift transport.
  this.transport.open()

  override def close(): Unit =
    this.transport.close()

  override def execute(transaction: thrift.Transaction): Try[thrift.Literal] =
    Try(this.underlying.execute(transaction))

}

object Client {

  /**
   *
   * @param instance
   * @return
   */
  def apply(instance: Instance): Client = {
    val transport = new TSocket(instance.host, instance.port)
    val protocol = new TBinaryProtocol(transport)
    val underlying = new thrift.Database.Client(protocol)
    Client(transport, underlying)
  }

}