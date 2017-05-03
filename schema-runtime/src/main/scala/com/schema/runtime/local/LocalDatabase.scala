package com.schema.runtime
package local

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
 * An in-memory database. Local databases delegate accesses and modifications to an underlying
 * mutable map implementation and are particularly useful for testing applications that rely on an
 * arbitrary database implementation. Local databases are thread-safe if the underlying map
 * implementation thread safe.
 *
 * @param underlying Underlying mutable map.
 */
class LocalDatabase(underlying: mutable.Map[Key, (Long, Value)]) extends Database {

  override def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, (Long, Value)]] =
    this.underlying.synchronized {
      Future(this.underlying.filterKeys(keys).toMap)
    }

  override def put(depends: Map[Key, Long], changes: Map[Key, (Long, Value)])(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    this.underlying.synchronized {
      // Verify that all of the version conditions are satisfied in the underlying map. Otherwise,
      // return a conflict error back to the caller. Empty values are still recorded in the database
      // because its version number acts as a tombstone.
      if (depends.forall { case (k, v) => this.underlying.get(k).map(_._1).getOrElse(0L) <= v })
        Future(this.underlying ++= changes)
      else
        Future.failed(new Exception("Transaction conflict occurred."))
    }

}

object LocalDatabase {

  /**
   * Constructs an empty database backed by a [[TrieMap]]. Because keys will tend to have character
   * sequences in common, a trie will significantly reduce the memory overhead of the database.
   * Scala's [[TrieMap]] is a concurrent lock-free implementation of a hash array mapped trie.
   *
   * @return Empty local database.
   */
  def empty = new LocalDatabase(TrieMap.empty)

  /**
   * Constructs a database backed by a [[TrieMap]] that is initialized with the specified key value
   * pairs. Particularly useful for loading test data into the database.
   *
   * @param initial Initial key value pairs.
   * @return Local database initialized with the specified key value pairs.
   */
  def apply(initial: (Key, Value)*): LocalDatabase = {
    val underlying = initial.map { case (k, v) => (k, (0L, v)) }
    new LocalDatabase(TrieMap(underlying: _*))
  }

}
