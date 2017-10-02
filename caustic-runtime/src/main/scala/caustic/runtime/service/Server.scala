package caustic.runtime
package service

import caustic.common.concurrent.Process

import org.apache.thrift.server.TNonblockingServer
import org.apache.thrift.transport.TNonblockingServerSocket
import shapeless._

import java.net.InetAddress
import scala.concurrent.ExecutionContext

object Server {

  /**
   * Constructs a process that serves the specified database over the provided port.
   *
   * @param database Underlying database.
   * @param port Port number.
   * @param ec Implicit execution context.
   * @return Server process.
   */
  def apply(database: Database, port: Int)(
    implicit ec: ExecutionContext
  ): Process[Unit :: HNil] = {
    val transport = new TNonblockingServerSocket(port)
    val processor = new thrift.Database.AsyncProcessor(database)
    val arguments = new TNonblockingServer.Args(transport).processor(processor)
    val server = new TNonblockingServer(arguments)
    Process.sync(server.serve(), server.stop())
  }

  /**
   * Constructs a process that first announces the server in the registry and then serves the
   * specified database over the provided port.
   *
   * @param registry Instance registry.
   * @param database Underlying database.
   * @param port Port number.
   * @param ec Implicit execution context.
   * @return Announced server process.
   */
  def apply(registry: Registry, database: Database, port: Int)(
    implicit ec: ExecutionContext
  ): Process[Unit :: Unit :: HNil] = {
    val instance = Instance(InetAddress.getLocalHost.getHostName, port)
    val announce = Process.sync(registry.register(instance), registry.unregister(instance))
    announce before Server(database, port)
  }

}
