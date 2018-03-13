package caustic.cluster
package protocol

import caustic.cluster

import scala.collection.mutable

object Test {

  /**
   * An in-memory, collection of instances. Useful for testing.
   *
   * @param buffer Current members.
   */
  case class Cluster[C](
    service: Service[C],
    buffer: mutable.Set[Address]
  ) extends cluster.Cluster[C] {

    override def members: Set[Address] = this.buffer.toSet
    override def join(instance: Address): Unit = this.buffer += instance
    override def leave(instance: Address): Unit = this.buffer -= instance
    override def close(): Unit = ()

  }

  object Cluster {

    /**
     * Constructs an empty [[Test.Cluster]].
     *
     * @return Empty [[Test.Cluster]].
     */
    def empty[C](service: Service[C]): Test.Cluster[C] = {
      Test.Cluster[C](service, mutable.Set.empty[Address])
    }

    /**
     * Constructs a [[Test.Cluster]] with the specified initial instances.
     *
     * @param initial Initial instances.
     *
     * @return Initialized [[Test.Cluster]].
     */
    def apply[C](service: Service[C], initial: Address*): Test.Cluster[C] = {
      val cluster = Cluster.empty(service)
      initial.foreach(cluster.join)
      cluster
    }
  }

}
