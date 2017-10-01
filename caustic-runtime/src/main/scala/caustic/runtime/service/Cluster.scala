package caustic.runtime
package service

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache._

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.util.{Random, Try}

/**
 * A Thrift connection for clusters of instances. Thread-safe.
 *
 * @param instances Instance cache.
 * @param clients Client cache.
 */
case class Cluster(
  instances: PathChildrenCache,
  clients: mutable.Map[String, Client]
) extends Connection with PathChildrenCacheListener {

  // Setup the path cache.
  this.instances.getListenable.addListener(this)
  this.instances.start()

  override def close(): Unit = {
    // Avoid race by closing the cache first.
    this.instances.close()
    this.clients.values.foreach(_.close())
  }

  override def childEvent(curator: CuratorFramework, event: PathChildrenCacheEvent): Unit =
    event.getType match {
      case PathChildrenCacheEvent.Type.CHILD_ADDED | PathChildrenCacheEvent.Type.CHILD_UPDATED =>
        this.clients += event.getData.getPath -> Client(Instance(event.getData.getData))
      case PathChildrenCacheEvent.Type.CHILD_REMOVED =>
        this.clients.remove(event.getData.getPath).foreach(_.close())
      case _ =>
    }

  override def execute(transaction: thrift.Transaction): Try[thrift.Literal] = {
    // Avoids race by caching the available clients.
    val current = this.clients.values.toSeq
    val client = current(Random.nextInt(current.length))

    // Avoid race by synchronizing execution on the randomized client.
    client.synchronized {
      client.execute(transaction)
    }
  }

}

object Cluster {

  /**
   * Constructs a connection to the various servers in the specified registry.
   *
   * @param registry Server registry.
   * @return Cluster connection.
   */
  def apply(registry: Registry): Cluster = {
    val instances = new PathChildrenCache(registry.curator, registry.namespace, false)
    val clients = TrieMap.empty[String, Client]
    Cluster(instances, clients)
  }

}