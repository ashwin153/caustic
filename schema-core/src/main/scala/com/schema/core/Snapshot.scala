package com.schema.core

import scala.collection.mutable

/**
 * A mutable key-value cache. Snapshots are in-memory or on-disk caches that map unique string
 * identifiers to arbitrarily typed objects.
 */
trait Snapshot {

  def apply(key: String): Any = get(key).get

  def get(key: String): Option[Any]

  def contains(key: String): Boolean = get(key).isDefined

  def +=(kv: (String, Any)): Unit

  def -=(key: String): Unit

}

object Snapshot {

  /**
   * Constructs an empty snapshot from an empty [[LocalSnapshot]] backed by an empty mutable hash
   * map. Therefore, accesses and modifications on the default snapshot can be performed in O(1).
   *
   * @return Empty snapshot.
   */
  def empty: Snapshot = new LocalSnapshot(mutable.HashMap.empty)

  /**
   * An in-memory snapshot. Local snapshots delegate accesses and modifications to an underlying
   * mutable map implementation. Because local snapshots are stored on-heap, they are only useful in
   * situations with smaller data storage requirements.
   *
   * @param underlying Underlying mutable map.
   */
  class LocalSnapshot(underlying: mutable.Map[String, Any]) extends Snapshot {

    override def get(key: String): Option[Any] = this.underlying.get(key)

    override def +=(kv: (String, Any)): Unit = this.underlying += kv

    override def -=(key: String): Unit = this.underlying -= key

  }

}

