package caustic.common.collection

import scala.collection.mutable

/**
 * A wrapper around a Trie with Scala Map semantics.
 *
 * @param trie Underlying trie.
 */
case class TrieMap[K, V](trie: Trie[K, V]) extends mutable.Map[List[K], V] {

  override def get(key: List[K]): Option[V] = {
    this.trie.exactly(key).flatMap(_.value)
  }

  override def +=(kv: (List[K], V)): TrieMap.this.type = {
    this.trie.put(kv._1, kv._2)
    this
  }

  override def -=(key: List[K]): TrieMap.this.type = {
    this.trie.exactly(key).foreach(_.remove())
    this
  }

  override def iterator: Iterator[(List[K], V)] = {
    val rest = this.trie.children
      .filter(_.value.isDefined)
      .map(t => t.key -> t.value.get)

    this.trie.value match {
      case Some(v) => Iterator(this.trie.key -> v) ++ rest
      case None => rest
    }
  }

}
