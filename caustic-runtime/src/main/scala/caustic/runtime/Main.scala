package caustic.runtime

import java.io.File
import java.net.{URL, URLClassLoader}

/**
 * An entry-point that bootstraps and serves a Server. Server configurations are loaded from the
 * classpath, but may be overridden by providing a path to a configuration file path or by
 * explicitly setting the value of system properties.
 */
object Main extends App {

  if (args.length > 1) {
    println("Usage: ./pants run caustic-runtime/src/main/scala:server [config] -- -Dprop=value")
    System.exit(1)
  }

  // Add configuration file to classpath. https://stackoverflow.com/a/7884406/1447029
  if (args.nonEmpty) {
    val uri = new File(args(0)).toURI
    val classLoader = ClassLoader.getSystemClassLoader.asInstanceOf[URLClassLoader]
    val method = classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL])
    method.setAccessible(true)
    method.invoke(classLoader, Array(uri.toURL))
  }

  // Asynchronously bootstrap server, and tear it down on shutdown.
  val server = Server()
  sys.addShutdownHook {
    this.server.close()
  }

}