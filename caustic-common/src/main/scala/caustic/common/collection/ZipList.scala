package caustic.common.collection

import java.nio.ByteBuffer
import java.nio.charset.Charset

import scala.collection.mutable

/**
 * An immutable, Redis ZipList. ZipLists stores a sequence of strings in a single array of bytes,
 * each which each string is prefixed by its length. This allows the entire sequence to be stored in
 * a single, unescaped, buffer which can be stored in just one key-value pair in the database.
 * Loosely based on http://download.redis.io/redis-stable/src/ziplist.c.
 *
 * @param underlying Underlying text.
 * @param charset Default charset.
 */
case class ZipList(underlying: Seq[Byte], charset: Charset) {

  /**
   * Appends the element to the ZipList.
   *
   * @param element Element to append.
   * @return Concatenated ZipList.
   */
  def +(element: String): ZipList =
    this ++ Seq(element)

  /**
   * Appends all the elements in the sequence to the ZipList.
   *
   * @param elements Elements to append.
   * @return Concatenated ZipList.
   */
  def ++(elements: Iterable[String]): ZipList = {
    // Serialize the element to a ByteBuffer.
    val bytes = elements.map(_.getBytes(this.charset))
    val buffer = ByteBuffer.allocate(bytes.map(4 + _.length).sum)

    bytes.foreach { x =>
      buffer.putInt(x.length)
      buffer.put(x)
    }

    // Construct a ZipList from the bytes.
    ZipList(this.underlying ++ buffer.array(), this.charset)
  }

  /**
   * Converts the ZipList into a sequence of strings.
   *
   * @return Sequence representation.
   */
  def toSeq: Seq[String] = {
    val buffer = ByteBuffer.wrap(this.underlying.toArray)
    val builder = mutable.Buffer.empty[String]

    while (buffer.remaining() > 0) {
      val bytes = Array.ofDim[Byte](buffer.getInt())
      buffer.get(bytes)
      builder += new String(bytes, this.charset)
    }

    builder
  }

}

object ZipList {

  /**
   * Constructs an empty ZipList.
   *
   * @return Empty ZipList.
   */
  def empty: ZipList = ZipList(Seq.empty, Charset.defaultCharset())

  /**
   * Constructs a ZipList containing the specified elements.
   *
   * @param elements Initial elements.
   * @return Allocated ZipList.
   */
  def apply(elements: Iterable[String]): ZipList = ZipList.empty ++ elements

  /**
   * Constructs a ZipList containing the specified elements.
   *
   * @param elements Initial elements.
   * @return Allocated ZipList.
   */
  def apply(elements: String*): ZipList = ZipList.empty ++ elements

}
