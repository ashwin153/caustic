package caustic.service

import java.io.Closeable

/**
 * A remote service.
 */
trait Server extends Closeable {

  sys.addShutdownHook(this.close())

  /**
   * Returns the [[Address]] at which this [[Server]] is accessible.
   *
   * @return
   */
  def address: Address

  /**
   * Makes this [[Server]] accessible.
   */
  def serve(): Unit

}

object Server {

  /**
   * A discoverable [[Server]].
   *
   * @param cluster [[Cluster]] to announce in.
   * @param underlying Underlying [[Server]].
   */
  case class Announcer[Client](
    cluster: Cluster[Client],
    underlying: Server
  ) extends Server {

    override def address: Address = this.underlying.address

    override def serve(): Unit = {
      this.cluster.join(this.address)
      this.underlying.serve()
    }

    override def close(): Unit = {
      this.cluster.leave(this.address)
      this.underlying.close()
    }

  }

}