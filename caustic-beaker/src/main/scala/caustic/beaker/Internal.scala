package caustic.beaker

import caustic.beaker.concurrent.Executor
import caustic.beaker.thrift.{Ballot, Proposal, Revision}
import caustic.service.{Address, Announcer, Cluster}
import caustic.service.protocol.Thrift

import scala.collection.JavaConverters._

/**
 * An internal, Beaker implementation.
 */
object Internal {

  /**
   * An internal, Beaker service.
   */
  case object Service extends caustic.service.Service[Internal.Client] {

    private val underlying = Thrift.Service(new thrift.Beaker.Client.Factory())

    override def connect(address: Address): Internal.Client =
      Internal.Client(this.underlying.connect(address))

    override def disconnect(client: Internal.Client): Unit =
      this.underlying.disconnect(client.underlying)

  }

  /**
   * An internal, Beaker client. Each [[Beaker]] uses an [[Internal.Client]] to communicate with
   * the others. Supports operations that facilitate consensus, which are not safe to be made
   * externally visible.
   *
   * @param underlying Underlying [[Thrift.Client]].
   */
  case class Client(underlying: Thrift.Client[thrift.Beaker.Client]) {

    /**
     * Returns the latest known [[Revision]] of each key.
     *
     * @param keys Keys to get.
     * @return Latest known [[Revision]] of each key.
     */
    def get(keys: Set[Key]): Map[Key, Revision] =
      this.underlying.connection.get(keys.asJava).asScala.toMap

    /**
     * Prepares a [[Proposal]]. Beakers reply with a promise not to accept a conflicting
     * [[Proposal]] with a lower [[Ballot]]. If it has already made a promise to a conflicting
     * [[Proposal]] with a higher [[Ballot]], then the conflicting promise is returned. If it has
     * already accepted a conflicting [[Proposal]] at a lower ballot, then the conflicting proposal
     * is returned. Otherwise, the original [[Proposal]] is returned.
     *
     *
     * @param proposal
     * @return Promise
     */
    def prepare(proposal: Proposal): Proposal =
      this.underlying.connection.prepare(proposal)

    /**
     * Beakers accept any [[Proposal]] for which there does not exist a conflicting promise with a
     * higher [[Ballot]]. If the [[Proposal]] is accepted, the Beaker broadcasts its vote to learn
     * the [[Proposal]] to all members of the [[Cluster]].
     *
     * @param proposal
     * @return
     */
    def accept(proposal: Proposal): Unit =
      this.underlying.connection.accept(proposal)

    /**
     * Votes for a [[Proposal]]. A Beaker commits a [[Proposal]] once it receives a majority of
     * votes from the members of the [[Cluster]].
     *
     * @param proposal
     * @return
     */
    def learn(proposal: Proposal): Unit =
      this.underlying.connection.learn(proposal)

  }

}
