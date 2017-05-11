package com.schema.runtime
package local

import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}

/**
 * An in-memory, lock-free database. Implementation associates an atomic boolean with each key value
 * pair that represents whether the key is exclusively locked or not. Put operations attempt to
 * acquire all locks in their change set via a non-blocking, compare-and-set.
 *
 * @param underlying Underlying mutable map.
 */
class LockFreeDatabase(
  underlying: TrieMap[Key, (AtomicBoolean, (Revision, Value))]
) extends Database {

  override def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, (Revision, Value)]] =
    Future {
      keys.map(k => k -> this.underlying.get(k))
        .collect { case (k, Some(v)) => k -> v._2 }
        .toMap
    }

  override def put(depends: Map[Key, Revision], changes: Map[Key, (Revision, Value)])(
    implicit ec: ExecutionContext
  ): Future[Unit] = {
    // Attempt to acquire the lock associated with each key in the change set.
    val locks = changes.keySet.map { k =>
      val (lock, (_, _)) = this.underlying.getOrElseUpdate(k, (new AtomicBoolean(false), (0L, "")))
      k -> lock.compareAndSet(false, true)
    }

    if (locks.exists(!_._2) || depends.exists { case (k, r) => this.underlying.get(k).exists(_._2._1 != r) }) {
      // If there exists a lock that could not be acquired, then there is a write-write conflict and
      // if there exists a dependency whose revision has changed then the transaction conflicts with
      // a committed write. If it conflicts, release all acquired locks and return failure.
      locks.filter(_._2).foreach { case (k, _) => this.underlying(k)._1.set(false) }
      Future.failed(new Exception("Transaction conflicts"))
    } else {
      // Otherwise, apply all changes and release the various locks.
      this.underlying ++= changes.mapValues(x => (new AtomicBoolean(false), x))
      Future.unit
    }
  }

}

object LockFreeDatabase {

  /**
   *
   * @return
   */
  def empty: LockFreeDatabase = new LockFreeDatabase(TrieMap.empty)

  /**
   *
   * @param initial
   * @return
   */
  def apply(initial: (Key, Value)*): LockFreeDatabase = {
    val underlying = initial.map { case (k, v) => (k, (new AtomicBoolean(false), (0L, v))) }
    new LockFreeDatabase(TrieMap(underlying: _*))
  }

}
