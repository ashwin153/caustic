package caustic.service

import caustic.runtime.thrift

import org.apache.thrift.server.TNonblockingServer
import org.apache.thrift.transport.TNonblockingServerSocket

import java.io.Closeable
import java.net.InetAddress

/**
 * A Thrift, server instance.
 *
 * @param database Underlying database.
 * @param port Port number.
 */
case class Server(
  database: thrift.Database.AsyncIface,
  port: Int
) extends Closeable {

  // Construct a Thrift server and serve it asynchronously.
  val transport = new TNonblockingServerSocket(this.port)
  val processor = new thrift.Database.AsyncProcessor(this.database)
  val arguments = new TNonblockingServer.Args(this.transport).processor(this.processor)
  val server = new TNonblockingServer(this.arguments)
  val thread = new Thread(() => this.server.serve())

  // Tear down the Thrift server.
  override def close(): Unit = this.server.stop()

  /**
   * Returns the network location of the server.
   *
   * @return Network address.
   */
  def address: Address = Address(InetAddress.getLocalHost.getHostName, this.port)

  /**
   * Serves the server on a separate thread. Idempotent.
   */
  def serve(): Unit = if (!this.thread.isAlive) this.thread.start()

}

object Server {

  /**
   * Constructs a discoverable server instance.
   *
   * @param database Underlying database.
   * @param port Port number.
   * @param registry Instance registry.
   * @return Discoverable instance.
   */
  def apply(
    database: thrift.Database.AsyncIface,
    port: Int,
    registry: Registry
  ): Server =
    new Server(database, port) {
      override def serve(): Unit = {
        // Register the address of the server in the registry.
        registry.register(this.address)
        super.serve()
      }

      override def close(): Unit = {
        // Unregister the server and close it.
        registry.unregister(this.address)
        super.close()
      }
    }

}