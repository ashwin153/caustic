package caustic.runtime
package service

import java.net.InetAddress
import org.apache.thrift.server.TThreadPoolServer
import org.apache.thrift.transport.TNonblockingServerSocket

/**
 * A Thrift server. Thread-safe.
 *
 * @param underlying Underlying database.
 */
case class Server(underlying: Database) {

  /**
   * Serves the underlying database over the specified port. Blocks indefinitely.
   *
   * @param port Port number.
   */
  def serve(port: Int): Unit = {
    val transport = new TNonblockingServerSocket(port)
    val processor = new thrift.Database.AsyncProcessor(this.underlying)
    val arguments = new TThreadPoolServer.Args(transport).processor(processor)
    val server = new TThreadPoolServer(arguments)

    try {
      server.serve()
    } finally {
      server.stop()
      transport.close()
    }
  }

  /**
   * Registers the server instance in the provided registry, and then serves the underlying database
   * over the specified port. Blocks indefinitely.
   *
   * @param registry Instance registry.
   * @param port Port number.
   */
  def serve(registry: Registry, port: Int): Unit = {
    val instance = Instance(InetAddress.getLocalHost.getHostName, port)

    try {
      registry.register(instance)
      serve(port)
    } finally {
      registry.unregister(instance)
    }
  }

}