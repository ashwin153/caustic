package caustic.cluster

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

  override def address: Address = {
    this.underlying.address
  }

  override def serve(): Unit = {
    this.cluster.join(this.address)
    this.underlying.serve()
  }

  override def close(): Unit = {
    this.cluster.leave(this.address)
    this.underlying.close()
  }

}