package caustic.runtime
package memory

import com.github.benmanes.caffeine.{cache => caffeine}
import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
 * An in-memory, Caffeine cache.
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

}

object LocalCache {

  /**
   * Constructs an empty LocalCache backed by the specified database.
   *
   * @param database Underlying database.
   * @param size Maximum number of cache lines.
   * @param expires Expiration duration.
   * @return Empty LocalCache.
   */
  def empty(database: Database, size: Long, expires: Duration): LocalCache =
    LocalCache(database, caffeine.Caffeine.newBuilder()
      .maximumSize(size)
      .expireAfterAccess(expires.toMillis, TimeUnit.MILLISECONDS)
      .build[Key, Revision]()
    )

}