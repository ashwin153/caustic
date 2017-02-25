package com.schema.local

import com.schema.core.Snapshot
import scala.collection.mutable
import scala.collection.concurrent.TrieMap

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

object LocalSnapshot {

  /**
   * Constructs an empty snapshot backed by a [[TrieMap]]. Because schemas construct field
   * identifiers by prefixing the field name with the owning object identifier, we can store
   * identifiers in a compressed radix trie to significantly reduce the memory overhead of the
   * snapshot. Scala's [[TrieMap]] is lock-free and has O(1) accesses and modifications.
   *
   * @return Empty local snapshot.
   */
  def empty: Snapshot = new LocalSnapshot(TrieMap.empty)


}

