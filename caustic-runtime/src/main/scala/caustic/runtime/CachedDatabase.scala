package caustic.runtime

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * A transactional, cached key-value store. CachedDatabases maintain a write-through cache, that is
 * kept consistent with the underlying database by automatically invalidating keys that participate
 * in failed transactions and updating keys that are modified in successful ones.
 *
 * @param cache Write-through cache.
 * @param database Underlying storage.
 */
case class CachedDatabase(cache: Cache, database: Database) extends Database {

  override def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, Revision]] =
    // Determine if there are any cache misses.
    this.cache.get(keys) flatMap { hits =>
      val misses = hits.keySet diff keys
      if (misses.nonEmpty) {
        // Reload any cache misses from the underlying database.
        this.database.get(misses) flatMap  { updates =>
          this.cache.put(updates).map(_ => hits ++ updates)
        }
      } else {
        // Return the cache hits otherwise.
        Future(hits)
      }
    }

  override def cput(depends: Map[Key, Version], changes: Map[Key, Revision])(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    this.database.cput(depends, changes) transformWith {
      case Success(_) =>
        // Update the values of changed keys.
        this.cache.put(changes)
      case Failure(e) =>
        // Invalidate cached keys to force them to reload from the database.
        this.cache.invalidate(depends.keySet union changes.keySet).transform(_ => Failure(e))
    }

}
