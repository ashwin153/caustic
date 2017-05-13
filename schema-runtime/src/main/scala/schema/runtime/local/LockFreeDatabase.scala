package schema.runtime
package local

import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}

/**
 * An in-memory, lock-free database.
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
    // Attempt to acquire the write lock associated with each key in the change set in parallel
    // using a non-blocking compare-and-set.
    val locks = changes.keySet.par.map { k =>
      val (lock, _) = this.underlying.getOrElseUpdate(k, (new AtomicBoolean(false), (0L, "")))
      k -> lock.compareAndSet(false, true)
    }

    // if there exists a dependency whose revision has changed then the transaction conflicts with
    // a committed write. If it conflicts, release all acquired locks and return failure. Otherwise,
    // apply all changes. Then, release all locks that the transaction acquired.
    if (locks.exists(!_._2) || depends.exists { case (k, r) => this.underlying.get(k).exists(_._2._1 != r) }) {
      locks.filter(_._2).foreach { case (k, _) => this.underlying(k)._1.set(false) }
      Future.failed(new Exception("Transaction conflicts."))
    } else {
      this.underlying ++= changes.mapValues(x => (new AtomicBoolean(false), x))
      Future.unit
    }
  }

}

object LockFreeDatabase {

  /**
   * Constructs an empty database.
   *
   * @return Empty database.
   */
  def empty: LockFreeDatabase = new LockFreeDatabase(TrieMap.empty)

  /**
   * Constructs a database with the initial key-value pairs.
   *
   * @param initial Initial key-value pairs.
   * @return Initialized lock-free database.
   */
  def apply(initial: (Key, Value)*): LockFreeDatabase = {
    val underlying = initial.map { case (k, v) => (k, (new AtomicBoolean(false), (0L, v))) }
    new LockFreeDatabase(TrieMap(underlying: _*))
  }

}
