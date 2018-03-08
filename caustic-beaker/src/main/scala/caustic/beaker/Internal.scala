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
     * Prepares a proposal. If a beaker has not made apromise to a newer proposal, it responds with
     * a promise. When a beaker makes a promise, it refuses to accept any proposal that conflicts
     * with the proposal it returns that has a lower ballot than the proposal it receives. If a
     * beaker has already accepted older proposals, it merges them together and returns the result.
     * Otherwise, it returns the proposal with a zero ballot.
     *
     * @param proposal Proposal to prepare.
     * @return Promise or the ballot of any newer promise that it has made.
     */
    def prepare(proposal: Proposal): Proposal =
      this.underlying.connection.prepare(proposal)

    /**
     * Accepts a proposal. Beakers accept a proposal if they have not promised not to. If a beaker
     * accepts a proposal, it discards all older accepted proposals and broadcasts a vote for it.
     *
     * @param proposal Proposal to accept.
     */
    def accept(proposal: Proposal): Unit =
      this.underlying.connection.accept(proposal)

    /**
     * Votes for a proposal. Beakers learn a proposal once a majority of beakers vote for it. If a
     * beaker learns a proposal, it commits its transactions and repairs on its replica of the
     * database.
     *
     * @param proposal Proposal to learn.
     */
    def learn(proposal: Proposal): Unit =
      this.underlying.connection.learn(proposal)

  }

}
