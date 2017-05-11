package com.schema.runtime
package local

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}

/**
 * An in-memory, synchronous database.
 *
 * @param underlying Underlying mutable map.
 */
class SynchronizedDatabase(
  underlying: TrieMap[Key, (Revision, Value)]
) extends Database {

  override def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, (Revision, Value)]] =
    Future {
      keys.map(k => k -> this.underlying.get(k))
        .collect { case (k, Some(v)) => k -> v }
        .toMap
    }

  override def put(depends: Map[Key, Revision], changes: Map[Key, (Revision, Value)])(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    synchronized {
      if (depends.exists { case (k, r) => this.underlying.get(k).exists(_._1 != r) })
        Future.failed(new Exception("Transaction conflicts"))
      else
        Future(this.underlying ++= changes)
    }

}

object SynchronizedDatabase {

  /**
   *
   * @return
   */
  def empty: SynchronizedDatabase = new SynchronizedDatabase(TrieMap.empty)

  /**
   *
   * @param initial
   * @return
   */
  def apply(initial: (Key, Value)*): SynchronizedDatabase = {
    val underlying = initial.map { case (k, v) => (k, (0L, v)) }
    new SynchronizedDatabase(TrieMap(underlying: _*))
  }

}
