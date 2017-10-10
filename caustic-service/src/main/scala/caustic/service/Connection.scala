package caustic.service

import caustic.runtime.thrift

import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.{TFramedTransport, TSocket}

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
 * A Thrift connection for server instances. Not thread-safe.
 *
 * @param host Instance hostname.
 * @param port Instance port number.
 */
case class Connection(
  host: String,
  port: Int
) extends Client {

  // Setup the Thrift transport.
  val transport = new TFramedTransport(new TSocket(this.host, this.port))
  val protocol = new TBinaryProtocol(transport)
  val underlying = new thrift.Database.Client(protocol)
  this.transport.open()

  override def execute(
    transaction: thrift.Transaction,
    backoffs: Seq[FiniteDuration]
  ): Try[thrift.Literal] =
    Try(this.underlying.execute(transaction, backoffs.map(_.toMillis).map(long2Long).asJava))

  override def close(): Unit =
    this.transport.close()

}
