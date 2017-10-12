package caustic.service.server

import caustic.runtime.Server
import com.typesafe.config.ConfigFactory

/**
 * A standalone server. Standalone servers are only accessible by clients with prior knowledge of
 * their network location. They are suitable for small, single-server deployments.
 */
object Standalone extends App {

  // Print usage information and fail fast for invalid arguments.
  if (args.length > 1) {
    println("Usage: ./pants run caustic-service/src/main/scala:standalone configuration")
    System.exit(1)
  }

  // Load the default configuration, or load the provided configuration.
  val config = if (args.isEmpty) ConfigFactory.load() else ConfigFactory.load(args(0))
  val server = Server(this.config)

  // Teardown on shutdown.
  sys.addShutdownHook {
    this.server.close()
  }

}
