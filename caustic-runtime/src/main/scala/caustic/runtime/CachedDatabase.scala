package caustic.runtime

import caustic.common.concurrent.Lock

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, blocking}

/**
 * An in-memory, thread-safe database. Provides transactional accesses and modifications to the data
 * stored in the underlying mutable map. Permits concurrent reads, but requires exclusive writes.
 *
 * @param underlying Underlying data store.
 */
case class CachedDatabase(underlying: mutable.Map[Key, Revision]) extends Database {

  private[this] val lock = Lock()

  override def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, Revision]] =
    Future {
      blocking {
        this.lock.shared {
          keys.map(k => k -> this.underlying.get(k))
            .collect { case (k, Some(v)) => k -> v }
            .toMap
        }
      }
    }

  override def put(depends: Map[Key, Version], changes: Map[Key, Revision])(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    Future {
      blocking {
        this.lock.exclusive {
          if (depends.exists { case (k, v) => this.underlying.get(k).exists(_.version > v) })
            throw new Exception("Transaction conflicts.")
          else
            this.underlying ++= changes
        }
      }
    }

}

object CachedDatabase {

  /**
   * Constructs an empty, in-memory database backed by an empty mutable map.
   *
   * @return Empty CachedDatabase.
   */
  def empty: CachedDatabase = CachedDatabase(mutable.Map.empty)

}