package caustic.runtime
package service

import java.nio.charset.Charset

/**
 * A server instance.
 *
 * @param host Hostname.
 * @param port Port number.
 */
case class Instance(
  host: String,
  port: Int
) {

  /**
   * Serializes the instance to a sequence of bytes.
   *
   * @return Serialized representation.
   */
  def toBytes: Array[Byte] =
    s"${this.host}:${this.port}".getBytes(Charset.forName("UTF-8"))

}

object Instance {

  /**
   * Constructs an instance from the serialized bytes.
   *
   * @param bytes Serialized bytes.
   * @return Instance.
   */
  def apply(bytes: Array[Byte]): Instance = {
    val tokens = new String(bytes, Charset.forName("UTF-8")).split(":")
    Instance(tokens(0), tokens(1).toInt)
  }

}