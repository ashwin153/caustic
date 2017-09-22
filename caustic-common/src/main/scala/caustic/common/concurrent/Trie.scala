package caustic.common.concurrent

import scala.collection.mutable

/**
 * A simple implementation of a concurrent, generalized trie. A trie or prefix tree, is a data
 * structure optimized for prefix lookups. Unlike other trie implementations like the well-known
 * PatriciaTrie, this implementation makes no claim to be space-optimized; each node of the trie
 * stores exactly one symbol (except the root, which contains no symbol) and an optional value.
 *
 * @param parent Parent node or None if root.
 * @param symbol Optional symbol stored at node.
 * @param value Optional value stored at node.
 * @tparam K Type of symbols.
 * @tparam V Type of values.
 */
class Trie[K, V] private (
  val parent: Option[Trie[K, V]],
  val symbol: Option[K],
  var value: Option[V]
) {

  private[Trie] val next = new mutable.HashMap[K, Trie[K, V]]
  private[Trie] val lock = Lock()

  /**
   * Returns the full key associated with this trie. The key is simply the concatenation of all the
   * symbols of all the trie's ancestors. Implementation ignores all empty symbols. Because symbols
   * are immutable, this method is thread-safe. O(h), where h is the height of the trie.
   *
   * @return Concatenation of all symbols of all ancestors.
   */
  def key: List[K] = (parent, symbol) match {
    case (None, None) => Nil
    case (None, Some(name)) => name :: Nil
    case (Some(trie), None) => trie.key
    case (Some(trie), Some(name)) => trie.key :+ name
  }

  /**
   * Returns the trie with the closest matching key. Implementation is thread-safe, but requires a
   * shared lock for each trie node corresponding to a symbol in the key. O(k), where k is the
   * length of the key.
   *
   * @param symbols Sequence of symbols to search for.
   * @return Trie with the closest matching key.
   */
  def closest(symbols: List[K]): Trie[K, V] = symbols match {
    case x :: rest => this.lock.optimistic(this.next.get(x)).fold(this)(_.closest(rest))
    case Nil => this
  }

  /**
   * Returns an option containing the trie with an exactly matching key or None. Implementation is
   * thread-safe, but requires a shared lock for each trie node corresponding to a symbol in the
   * key. O(k), where k is the length of the key.
   *
   * @param symbols Sequence of symbols to search for.
   * @return Trie with the closest matching key.
   */
  def exactly(symbols: List[K]): Option[Trie[K, V]] = symbols match {
    case x :: rest => this.lock.optimistic(this.next.get(x)).map(_.exactly(rest)).getOrElse(None)
    case Nil => Some(this)
  }

  /**
   * Traverses through the specified key (sequence of symbols), applying the visitor function to
   * each trie node. The visitor function specifies a new value for a node, given the key suffix
   * (remaining symbols in the key) and the previous value. The visitor function is guaranteed to be
   * applied atomically. Implementation is thread-safe, but requires an exclusive lock for each
   * visited trie node. O(k O(visitor)), where k is the length of the key.
   *
   * @param symbols Sequence of symbols to insert.
   * @param visitor Function to apply to each visited trie node.
   */
  def put(symbols: List[K], visitor: (List[K], Option[V]) => Option[V]): Unit =
    symbols match {
      case Nil => this.lock.exclusive { this.value = visitor(Nil, this.value) }
      case x :: rest =>
        this.lock.exclusive {
          this.value = visitor(symbols, this.value)
          this.next.getOrElseUpdate(x, new Trie(Some(this), Some(x), None))
        }.put(rest, visitor)
    }

  def put(symbols: List[K], value: V): Unit =
    put(symbols, (suffix, prev) => (suffix, prev) match {
      case (Nil, _) => Some(value)
      case _ => prev
    })

  /**
   * Recursively removes this node and all its children, by removing the trie from its parent's list
   * of children. Implementation is thread-safe, but requires an exclusive write-lock. O(h), where
   * h is the height of the trie.
   */
  def remove(): Unit =
    parent.foreach(trie => this.symbol.foreach {
      trie.lock.exclusive(trie.next.remove(_))
    })

  /**
   * Returns the set of all children of this trie. Modifications to this set have no effect on the
   * underlying trie.
   *
   * @return Set of all children of this trie.
   */
  def children: Iterator[Trie[K, V]] =
    this.lock.optimistic(this.next.values.iterator)

  /**
   * Returns a string representation of the trie. Prints the symbol and value for each node of the
   * trie, but indents the output for children by a tab. Useful for debugging purposes. For example,
   * the output might look like:
   *
   * ("h", 15)
   *   ("e", 15)
   *     ("l", 7)
   *       ("l", 3)
   *       ("p", 4)
   *     ("y", 3)
   *
   * @return String representation of the trie.
   */
  override def toString: String = {
    val builder = new mutable.StringBuilder((this.symbol, this.value).toString)
    this.children.foreach(child => builder.append("\n" + child.toString))
    builder.toString.replaceAll("\n", "\n\t")
  }

}

object Trie {

  /**
   * Construct an empty trie.
   *
   * @tparam K Type of keys.
   * @tparam V Type of values.
   * @return An empty trie.
   */
  def empty[K, V]: Trie[K, V] = new Trie[K, V](None, None, None)

}