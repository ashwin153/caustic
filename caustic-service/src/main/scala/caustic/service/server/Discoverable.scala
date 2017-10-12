package caustic.service.server

import caustic.runtime.Server
import caustic.service.discovery.{Address, Registry}

import com.typesafe.config.ConfigFactory

import java.net.InetAddress

/**
 * A discoverable server. Discoverable servers automatically register themselves in a registry that
 * clients can use to dynamically discover the network location of the various servers in a cluster.
 * This is particularly useful for larger deployments that require greater availability or
 * throughput than a single standalone instance can provide.
 */
object Discoverable extends App {

  // Print usage information and fail fast for invalid arguments.
  if (args.isEmpty || args.length > 3) {
    println("Usage: ./pants run caustic-service/src/main/scala:discoverable configuration")
    System.exit(1)
  }

  // Load the default configuration, or load the provided configuration.
  val config = if (args.isEmpty) ConfigFactory.load() else ConfigFactory.load(args(0))
  val server = Server(this.config)

  // Register the server.
  val registry = Registry(this.config)
  val address = Address(InetAddress.getLocalHost.getHostAddress, this.server.port)
  this.registry.register(this.address)

  // Teardown on shutdown.
  sys.addShutdownHook {
    this.registry.unregister(this.address)
    this.server.close()
    this.registry.close()
  }

}
