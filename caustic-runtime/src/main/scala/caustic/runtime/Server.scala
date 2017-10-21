package caustic.runtime

import caustic.runtime.service.{Address, Registry}
import caustic.runtime.service._
import caustic.runtime.jdbc.JdbcDatabase
import caustic.runtime.local.{LocalCache, LocalDatabase}
import caustic.runtime.redis.RedisCache
import org.apache.thrift.server.TNonblockingServer
import org.apache.thrift.transport.TNonblockingServerSocket
import pureconfig._
import java.io.Closeable

/**
 *
 * @param database
 * @param port
 * @param registry
 */
case class Server(
  database: Database,
  port: Int,
  registry: Option[Registry] = scala.None
) extends Closeable {

  // Construct a Thrift server.
  private val transport = new TNonblockingServerSocket(this.port)
  private val processor = new thrift.Database.AsyncProcessor(this.database)
  private val arguments = new TNonblockingServer.Args(this.transport).processor(this.processor)
  private val server = new TNonblockingServer(this.arguments)
  private val thread = new Thread(() => this.server.serve())

  // Asynchronously serve the database.
  this.thread.start()

  // Announce the server in the registry.
  this.registry.foreach(_.register(Address.local(this.port)))

  /**
   *
   * @return
   */
  def address: Address = Address.local(this.port)

  override def close(): Unit = {
    // Remove the server from the registry.
    this.registry.foreach(_.unregister(Address.local(this.port)))

    // Close the Thrift server and underlying database.
    this.server.stop()
    this.database.close()
  }

}

object Server {

  /**
   *
   * @param port
   * @param caches
   * @param database
   * @param discoverable
   */
  case class Config(
    port: Int,
    caches: List[String],
    database: String,
    discoverable: Boolean
  )

  /**
   *
   * @return
   */
  def apply(): Server =
    Server(loadConfigOrThrow[Config]("caustic.server"))

  /**
   *
   * @param config
   * @return
   */
  def apply(config: Config): Server = {
    // Setup the underlying database and caches.
    val underlying = config.caches.foldRight {
      config.database match {
        case "local" => LocalDatabase()
        case "jdbc" => JdbcDatabase()
      }
    } { case (cache, db) =>
      cache match {
        case "local" => LocalCache(db)
        case "redis" => RedisCache(db)
      }
    }

    // Setup a server and optionally bootstrap a registry.
    val registry = Option(config.discoverable) collect { case true => Registry() }
    Server(underlying, config.port, registry)
  }

}