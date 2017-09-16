package caustic.runtime
package memory

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
 * An in-memory, database. LocalDatabase are thread-safe if and only if the underlying mutable map
 * is thread-safe. Provides transactional accesses and modifications to the data stored in the
 * underlying mutable map.
 *
 * @param underlying Underlying data store.
 */
case class LocalDatabase(underlying: mutable.Map[Key, Revision]) extends Database {

  override def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, Revision]] =
    Future {
      keys.map(k => k -> this.underlying.get(k))
        .collect { case (k, Some(v)) => k -> v }
        .toMap
    }

  override def cput(depends: Map[Key, Version], changes: Map[Key, Revision])(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    Future {
      if (depends.exists { case (k, v) => this.underlying.get(k).exists(_.version > v) })
        throw new Exception("Transaction conflicts.")
      else
        this.underlying ++= changes
    }

}

object LocalDatabase {

  /**
   *
   * @return
   */
  def empty: LocalDatabase = LocalDatabase(mutable.Map.empty)

}