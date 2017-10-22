package caustic.runtime.redis

import caustic.runtime.{Cache, Database, Key, Revision}
import caustic.runtime.redis.RedisCache._

import akka.actor.ActorSystem
import akka.util.ByteString
import pureconfig._
import redis.{ByteStringDeserializer, ByteStringSerializer, RedisClient}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import scala.concurrent.{ExecutionContext, Future}

/**
 * A Redis-backed, cache. Thread-safe.
 *
 * @param database Underlying database.
 * @param client Redis client.
 */
case class RedisCache(
  database: Database,
  client: RedisClient
) extends Cache {

  override def fetch(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, Revision]] = {
    val seq = keys.toSeq
    this.client.mget(seq: _*)
      .map(values => seq.zip(values) collect { case (k, Some(r)) => k -> r })
      .map(_.toMap)
  }

  override def update(changes: Map[Key, Revision])(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    this.client.mset(changes).map(_ => Unit)

  override def invalidate(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    this.client.del(keys.toSeq: _*).map(_ => Unit)

  override def close(): Unit = {
    this.client.stop()
    super.close()
  }

}

object RedisCache {

  // Redis Serializer.
  implicit val serializer: ByteStringSerializer[Revision] = revision => {
    val bytes = new ByteArrayOutputStream()
    val stream = new ObjectOutputStream(bytes)
    stream.writeObject(revision)
    bytes.close()
    ByteString(bytes.toByteArray)
  }

  // Redis Deserializer.
  implicit val deserializer: ByteStringDeserializer[Revision] = repr => {
    val bytes = new ByteArrayInputStream(repr.toArray)
    val stream = new ObjectInputStream(bytes)
    val result = stream.readObject().asInstanceOf[Revision]
    bytes.close()
    result
  }

  // Implicit Actor System.
  implicit val system: ActorSystem = ActorSystem.create()
  sys.addShutdownHook(this.system.terminate())

  /**
   * A RedisCache configuration.
   *
   * @param host Redis hostname.
   * @param port Redis port number.
   * @param password Optional password.
   */
  case class Config(
    host: String,
    port: Int,
    password: Option[String]
  )

  /**
   * Constructs a RedisCache by loading the configuration from the classpath.
   *
   * @param database Underlying database.
   * @return Classpath-configured RedisCache.
   */
  def apply(database: Database): RedisCache =
    RedisCache(database, loadConfigOrThrow[Config]("caustic.cache.redis"))

  /**
   * Constructs a RedisCache from the provided configuration.
   *
   * @param database Underlying database.
   * @param config Configuration.
   * @return Dynamically-configured RedisCache.
   */
  def apply(database: Database, config: Config): RedisCache =
    RedisCache(database, RedisClient(config.host, config.port, config.password))

}