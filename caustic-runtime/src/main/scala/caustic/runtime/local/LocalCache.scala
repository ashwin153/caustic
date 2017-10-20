package caustic.runtime
package local

import com.github.benmanes.caffeine.{cache => caffeine}
import com.typesafe.config.Config
import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
 * An in-memory, Caffeine cache. Thread-safe.
 *
 * @param database Underlying database.
 */
case class LocalCache(
  database: Database,
  underlying: caffeine.Cache[Key, Revision]
) extends Cache {

  override def fetch(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, Revision]] =
    Future(this.underlying.getAllPresent(keys.asJava).asScala.toMap)

  override def update(changes: Map[Key, Revision])(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    Future(this.underlying.putAll(changes.asJava))

  override def invalidate(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    Future(this.underlying.invalidateAll(keys.asJava))

  override def close(): Unit = {
    this.underlying.invalidateAll()
    super.close()
  }

}

object LocalCache {

  // Configuration Root.
  val root: String = "caustic.runtime.cache.local"

  /**
   * Constructs a LocalCache backed by the specified database.
   *
   * @param database Underlying database.
   * @param capacity Maximum size in bytes.
   * @param expiration Expiration duration.
   * @return LocalCache.
   */
  def apply(database: Database, capacity: Long, expiration: Duration): LocalCache =
    LocalCache(database, caffeine.Caffeine.newBuilder()
      .weigher((k: Key, r: Revision) => sizeof(k) + sizeof(r))
      .maximumWeight(capacity)
      .expireAfterAccess(expiration.toMillis, TimeUnit.MILLISECONDS)
      .build[Key, Revision]())

  /**
   *
   * @param database
   * @param config
   * @return
   */
  def apply(database: Database, config: Config): LocalCache = {
    val capacity = config.getBytes(s"$root.capacity")
    val expiration = Duration.fromNanos(config.getDuration(s"$root.expiration").toNanos)
    LocalCache(database, capacity, expiration)
  }

  /**
   * Returns the approximate size of the key-revision pair. In-memory caches are typically bounded
   * in size by a maximum number of entries, after which an eviction protocol (eg. LRU) kicks in to
   * trim the size of the cache. However, this approach only works for homogenous-entry caches (ie.
   * fixed length), because otherwise the actual memory utilization of the cache would grow
   * proportionally with the total length of its contents. Instead, we may exploit empirical results
   * (see https://github.com/ashwin153/sandbox/tree/master/footprint) about the length of cache 
   * entries to instead bound the size of the cache by its total memory utilization. This will lead 
   * to far more predictable sizes for heterogenous-entry caches.
   *
   * @param x Object.
   * @return Approximate size in bytes.
   */
  def sizeof(x: Any): Int = x match {
    case x: String => 40 + math.ceil(x.length / 4.0).toInt * 8
    case _: Flag => 16
    case _: Real => 24
    case x: Text => 16 + sizeof(x.value)
    case x: Revision => 24 + sizeof(x.value)
  }

}
