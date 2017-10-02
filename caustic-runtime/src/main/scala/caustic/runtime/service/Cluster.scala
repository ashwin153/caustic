package caustic.runtime
package service

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache._
import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Random, Try}

/**
 * A Thrift connection for clusters of instances. Thread-safe.
 *
 * @param instances Instance cache.
 * @param connections Connections cache.
 */
case class Cluster(
  instances: PathChildrenCache,
  connections: mutable.Map[String, Connection]
) extends Client with PathChildrenCacheListener {

  // Setup the path cache.
  this.instances.getListenable.addListener(this)
  this.instances.start()

  override def close(): Unit = {
    // Avoid race by closing the cache first.
    this.instances.close()
    this.connections.values.foreach(_.close())
  }

  override def childEvent(curator: CuratorFramework, event: PathChildrenCacheEvent): Unit =
    event.getType match {
      case PathChildrenCacheEvent.Type.CHILD_ADDED | PathChildrenCacheEvent.Type.CHILD_UPDATED =>
        this.connections += event.getData.getPath -> Connection(Instance(event.getData.getData))
      case PathChildrenCacheEvent.Type.CHILD_REMOVED =>
        this.connections.remove(event.getData.getPath).foreach(_.close())
      case _ =>
    }

  override def execute(
    transaction: thrift.Transaction,
    backoffs: Seq[FiniteDuration]
  ): Try[thrift.Literal] = {
    // Avoids race by caching the available clients.
    val current = this.connections.values.toSeq
    if (current.isEmpty) {
      // If there are no available clients, then throw an error.
      Failure(new IndexOutOfBoundsException("No available servers."))
    } else {
      // Avoid race by synchronizing execution on the randomized client.
      val client = current(Random.nextInt(current.length))
      client.synchronized {
        client.execute(transaction, backoffs)
      }
    }
  }

}

object Cluster {

  /**
   * Constructs a connection to the various servers in the specified registry.
   *
   * @param registry Server registry.
   * @return Cluster client.
   */
  def apply(registry: Registry): Cluster = {
    val instances = new PathChildrenCache(registry.curator, registry.namespace, true)
    val connections = TrieMap.empty[String, Connection]
    Cluster(instances, connections)
  }

}