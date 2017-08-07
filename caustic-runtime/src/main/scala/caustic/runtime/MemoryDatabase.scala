package caustic.runtime

import caustic.common.concurrent.Lock
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
 * An in-memory, synchronized database. Provides thread-safe accesses and modifications to the data
 * stored in an underlying mutable map. Memory databases permit concurrent reads, but require
 * exclusive writes.
 *
 * @param underlying Underlying data store.
 */
case class MemoryDatabase(underlying: mutable.Map[Key, Revision]) extends TransactionalDatabase {

  private[this] val lock: Lock = Lock()

  override def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, Revision]] =
    Future {
      this.lock.shared {
        keys.map(k => k -> this.underlying.get(k))
          .collect { case (k, Some(v)) => k -> v }
          .toMap
      }
    }

  override def put(depends: Map[Key, Version], changes: Map[Key, Revision])(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    Future {
      this.lock.exclusive {
        val conflicts = depends.exists { case (key, version) =>
          val current = this.underlying.get(key).map(_.version).getOrElse(0L)
          current >= version
        }

        if (conflicts)
          Future.failed(WriteException("Transaction conflicts."))
        else
          Future(this.underlying ++= changes)
      }
    }

}