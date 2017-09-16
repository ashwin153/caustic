package caustic.runtime
package memory

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
 *
 * @param underlying
 */
case class LocalCache(underlying: mutable.Map[Key, Revision]) extends Cache {

  override def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, Revision]] =
    Future {
      keys.map(k => k -> this.underlying.get(k))
        .collect { case (k, Some(v)) => k -> v }
        .toMap
    }

  override def put(changes: Map[Key, Revision])(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    Future(this.underlying ++= changes)


  override def invalidate(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    Future(this.underlying --= keys)

}

object LocalCache {

  /**
   *
   * @return
   */
  def empty: LocalCache = LocalCache(mutable.Map.empty)

}