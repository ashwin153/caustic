package caustic.beaker

import caustic.beaker.thrift._
import caustic.cluster
import caustic.cluster.Address
import caustic.cluster.protocol.Thrift

import scala.collection.JavaConverters._

/**
 * An internal, Beaker implementation.
 */
object Internal {

  /**
   * An internal, Beaker service.
   */
  case object Service extends cluster.Service[Internal.Client] {

    private val underlying = Thrift.Service(new thrift.Beaker.Client.Factory())

    override def connect(address: Address): Internal.Client =
      Internal.Client(this.underlying.connect(address))

    override def disconnect(client: Internal.Client): Unit =
      this.underlying.disconnect(client.underlying)

  }

  /**
   * An internal, Beaker client. Beakers use the client to communicate with each other. Supports
   * operations that facilitate consensus, which are not safe to be made externally visible.
   *
   * @param underlying Underlying Thrift client.
   */
  case class Client(underlying: Thrift.Client[thrift.Beaker.Client]) {

    /**
     * Returns the latest known revision of each key.
     *
     * @param keys Keys to get.
     * @return Revision of each key.
     */
    def get(keys: Set[Key]): Map[Key, Revision] =
      this.underlying.connection.get(keys.asJava).asScala.toMap

    /**
     * Prepares a proposal. If a beaker has not made a promise to a newer proposal, it responds with
     * a promise. When a beaker makes a promise, it refuses to accept any proposal that conflicts
     * with the proposal it returns that has a lower ballot than the proposal it receives. If a
     * beaker has already accepted older proposals, it merges them together and returns the result.
     * Otherwise, it returns the proposal with a zero ballot.
     *
     * @param proposal Proposal to prepare.
     * @return Promise or the ballot of any newer promise that has been made.
     */
    def prepare(proposal: Proposal): Proposal =
      this.underlying.connection.prepare(proposal)

    /**
     *
     * @param proposal
     * @return
     */
    def accept(proposal: Proposal): Boolean =
      this.underlying.connection.accept(proposal)

    /**
     *
     * @param proposal
     */
    def share(proposal: Proposal): Unit =
      this.underlying.connection.share(proposal)

    /**
     *
     * @param proposal
     */
    def learn(proposal: Proposal): Unit =
      this.underlying.connection.learn(proposal)

  }

}
