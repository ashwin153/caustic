package caustic.runtime

import caustic.runtime.jdbc.JdbcDatabase
import caustic.runtime.local.{LocalCache, LocalDatabase}
import caustic.runtime.redis.RedisCache
import com.typesafe.config.Config
import org.apache.thrift.server.TNonblockingServer
import org.apache.thrift.transport.TNonblockingServerSocket
import java.io.Closeable
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

/**
 * A server instance.
 *
 * @param database Underlying database.
 * @param port Port number.
 */
case class Server(
  database: Database,
  port: Int
) extends Closeable {

  // Construct a Thrift server and serve it asynchronously.
  val transport = new TNonblockingServerSocket(this.port)
  val processor = new thrift.Database.AsyncProcessor(this.database)
  val arguments = new TNonblockingServer.Args(this.transport).processor(this.processor)
  val server = new TNonblockingServer(this.arguments)
  val thread = new Thread(() => this.server.serve())
  this.thread.start()

  override def close(): Unit = {
    // Close the Thrift server and underlying database.
    this.server.stop()
    this.database.close()
  }

}

object Server {

  /**
   * Constructs a server from the provided configuration.
   *
   * @param config Configuration.
   * @return Server instance.
   */
  def apply(config: Config): Server = {
    // Extract the runtime configuration.
    val runtime = config.getConfig("caustic.runtime")

    // Setup the underlying database and iteratively construct caches.
    val database = runtime.getString("server.database") match {
      case "local" => LocalDatabase(runtime.getConfig("database.local"))
      case "jdbc" => JdbcDatabase(runtime.getConfig("database.jdbc"))
    }

    val underlying = runtime.getStringList("server.caches").asScala.foldRight(database) {
      case ("local", db) => LocalCache(db, runtime.getConfig("cache.local"))
      case ("redis", db) => RedisCache(db, runtime.getConfig("cache.redis"))
    }

    // Construct a database.
    Server(underlying, runtime.getInt("server.port"))
  }

}