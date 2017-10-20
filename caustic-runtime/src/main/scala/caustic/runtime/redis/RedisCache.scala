package caustic.runtime.redis

import caustic.runtime.{Cache, Database, Key, Revision}
import caustic.runtime.redis.RedisCache._

import akka.actor.ActorSystem
import akka.util.ByteString
import com.typesafe.config.Config
import redis.{ByteStringDeserializer, ByteStringSerializer, RedisClient}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import scala.concurrent.{ExecutionContext, Future}

/**
 * A Redis-backed, cache.
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

  // Configuration Root.
  val root: String = "caustic.runtime.cache.redis"

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

  /**
   * Constructs a Redis client backed by the specified database.
   *
   * @param database Underlying database.
   * @param host Redis hostname.
   * @param port Redis port number.
   * @param password Optional Redis password.
   * @param system Implicit actor system.
   * @return RedisCache.
   */
  def apply(database: Database, host: String, port: Int, password: Option[String])(
    implicit system: ActorSystem
  ): RedisCache =
    RedisCache(database, RedisClient(host, port, password))

  /**
   *
   * @param database
   * @param config
   * @return
   */
  def apply(database: Database, config: Config): RedisCache = {
    val host = config.getString("host")
    val port = config.getInt("port")
    val password = config.getString("password")
    RedisCache(database, host, port, Some(password))
  }

}