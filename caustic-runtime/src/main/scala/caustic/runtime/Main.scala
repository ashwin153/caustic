package caustic.runtime

import com.typesafe.config.ConfigFactory
import scala.concurrent.Await

/**
 *
 */
object Main extends App {

  // Load the default configuration, or load the provided configuration.
  val config = if (args.isEmpty) ConfigFactory.load() else ConfigFactory.load(args(0))

  // Bootstrap a Caustic Server.
  val server = Server(this.config)

  // Teardown on shutdown.
  sys.addShutdownHook {
    this.server.close()
  }

}
