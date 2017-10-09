package caustic.runtime
package service

import caustic.common.concurrent.Process
import java.io.Closeable
import org.apache.thrift.server.TNonblockingServer
import org.apache.thrift.transport.TNonblockingServerSocket
import java.net.InetAddress
import scala.concurrent.{ExecutionContext, Future}
import shapeless._

/**
 * A Caustic, Thrift server.
 */
trait Server extends Closeable {

  /**
   * Serves the server in an asynchronous, background task.
   *
   * @return Background task.
   */
  def serve(): Future[Unit]

}

object Server {

  /**
   * Constructs a standalone server for the specified database over the provided port. Standalone
   * servers are only accessible by clients with prior knowledge of their network location.
   *
   * @param database Underlying database.
   * @param port Port number.
   * @param ec Implicit execution context.
   * @return Standalone Server.
   */
  def standalone(database: Database, port: Int)(
    implicit ec: ExecutionContext
  ): StandaloneServer = {
    val transport = new TNonblockingServerSocket(port)
    val processor = new thrift.Database.AsyncProcessor(database)
    val arguments = new TNonblockingServer.Args(transport).processor(processor)
    val server = new TNonblockingServer(arguments)
    StandaloneServer(Process.sync(server.serve(), server.stop()))
  }

  case class StandaloneServer(underlying: Process[Unit :: HNil])(
    implicit ec: ExecutionContext
  ) extends Server {
    override def serve(): Future[Unit] = this.underlying.start().map(_ => Unit)
    override def close(): Unit = this.underlying.stop()
  }

  /**
   * Constructs a discoverable server for the specified database over the provided port.
   * Discoverable servers are automatically registered in the provided registry, so that clients
   * can dynamically discover their network location.
   *
   * @param database Underlying database.
   * @param port Port number.
   * @param registry Instance registry.
   * @param ec Implicit execution context.
   * @return Discoverable Server.
   */
  def discoverable(database: Database, port: Int, registry: Registry)(
    implicit ec: ExecutionContext
  ): DiscoverableServer = {
    val instance = Instance(InetAddress.getLocalHost.getHostName, port)
    val announce = Process.sync(registry.register(instance), registry.unregister(instance))
    DiscoverableServer(announce before standalone(database, port).underlying)
  }

  case class DiscoverableServer(underlying: Process[Unit :: Unit :: HNil])(
    implicit ec: ExecutionContext
  ) extends Server {
    override def serve(): Future[Unit] = this.underlying.start().map(_ => Unit)
    override def close(): Unit = this.underlying.stop()
  }

}