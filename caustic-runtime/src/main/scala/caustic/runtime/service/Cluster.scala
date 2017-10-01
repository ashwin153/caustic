package caustic.runtime
package service

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache._

import scala.collection.mutable
import scala.util.{Random, Try}

/**
 * A Thrift connection for clusters of instances.
 *
 * @param instances Instance cache.
 * @param clients Client cache.
 */
case class Cluster(
  instances: PathChildrenCache,
  clients: mutable.Map[String, Client],
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
      case PathChildrenCacheEvent.Type.CHILD_ADDED =>
        this.clients += event.getData.getPath -> Client(Instance(event.getData.getData))
      case PathChildrenCacheEvent.Type.CHILD_UPDATED =>
        this.clients += event.getData.getPath -> Client(Instance(event.getData.getData))
      case PathChildrenCacheEvent.Type.CHILD_REMOVED =>
        this.clients.remove(event.getData.getPath).foreach(_.close())
    }

  override def execute(transaction: thrift.Transaction): Try[thrift.Literal] = {
    // Avoids race by caching the available clients.
    val current = this.clients.values.toSeq

    // Execute the transaction on a randomized client.
    val client = current(Random.nextInt(current.length))
    client.execute(transaction)
  }

}

object Cluster {

  /**
   *
   * @param registry
   * @return
   */
  def apply(registry: Registry): Cluster = {
    val instances = new PathChildrenCache(registry.curator, registry.path, false)
    val clients = mutable.Map.empty[String, Client]
    Cluster(instances, clients)
  }

}