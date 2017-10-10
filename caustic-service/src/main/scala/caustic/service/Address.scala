package caustic.service

import java.nio.charset.Charset

/**
 * A serializable, network location.
 *
 * @param host Hostname.
 * @param port Port number.
 */
case class Address(host: String, port: Int) {

  /**
   * Serializes the address to a sequence of bytes.
   *
   * @return Serialized representation.
   */
  def toBytes: Array[Byte] =
    s"${this.host}:${this.port}".getBytes(Address.Repr)

}

object Address {

  // Default byte representation.
  val Repr: Charset = Charset.forName("UTF-8")

  /**
   * Constructs an address from the serialized bytes.
   *
   * @param bytes Serialized representation.
   * @return Deserialized instance.
   */
  def apply(bytes: Array[Byte]): Address = {
    val tokens = new String(bytes, Address.Repr).split(":")
    Address(tokens(0), tokens(1).toInt)
  }

}