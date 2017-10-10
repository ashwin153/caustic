package caustic.runtime
package memory

import com.github.benmanes.caffeine.{cache => caffeine}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

/**
 * An in-memory, database. LocalDatabase are thread-safe if and only if the underlying mutable map
 * is thread-safe. Provides transactional accesses and modifications to the data stored in the
 * underlying mutable map.
 *
 * @param underlying Underlying data store.
 */
case class LocalDatabase(
  underlying: caffeine.Cache[Key, Revision]
) extends Database {

  override def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, Revision]] =
    Future(this.underlying.getAllPresent(keys.asJava).asScala.toMap)

  override def cput(depends: Map[Key, Version], changes: Map[Key, Revision])(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    Future {
      // Determine if the dependencies conflict with the underlying database.
      val conflicts = this.underlying
        .getAllPresent(depends.keys.asJava).asScala
        .filter { case (k, r) => depends(k) < r.version }

      // Throw an exception on conflict or perform updates otherwise.
      if (conflicts.isEmpty)
        this.underlying.putAll(changes.asJava)
      else
        throw ConflictException(conflicts.keySet.toSet)
    }

}

object LocalDatabase {

  /**
   * Constructs an empty, in-memory database.
   *
   * @return Empty LocalDatabase.
   */
  def empty: LocalDatabase = LocalDatabase(caffeine.Caffeine.newBuilder().build[Key, Revision]())

}