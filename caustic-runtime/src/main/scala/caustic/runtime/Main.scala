package caustic.runtime

import java.io.File
import java.net.{URL, URLClassLoader}

object Main extends App {

  if (args.length > 1) {
    println("Usage: ./pants run caustic-service/src/main/scala:server /path/to/config?")
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
