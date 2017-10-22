package caustic.runtime
package service

import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.{TFramedTransport, TSocket}

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
 * A direct connection to a server. Not thread-safe.
 *
 * @param address Server address.
 */
case class Connection(address: Address) extends Client {

  // Setup the Thrift transport.
  val transport = new TFramedTransport(new TSocket(this.address.host, this.address.port))
  val protocol = new TBinaryProtocol(transport)
  val underlying = new thrift.Database.Client(protocol)
  this.transport.open()

  override def execute(
    transaction: thrift.Transaction,
    backoffs: Seq[FiniteDuration]
  ): Try[thrift.Literal] =
    Try(this.underlying.execute(transaction, backoffs.map(_.toMillis).map(long2Long).asJava))

  override def close(): Unit = this.transport.close()

}

object Connection {

  /**
   * Constructs a Connection to the specified port on the local machine.
   *
   * @param port Port number.
   * @return Local Connection.
   */
  def apply(port: Int): Connection =
    Connection(Address.local(port))

}
