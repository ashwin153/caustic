package caustic.runtime
package service

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache._

import java.io.Closeable
import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Random, Try}

/**
 * A connection to all instances in a Registry. Thread-safe.
 *
 * @param instances ZooKeeper cache.
 * @param clients Established clients.
 */
case class Cluster(
  instances: PathChildrenCache,
  clients: mutable.Map[String, Client]
) extends Client with PathChildrenCacheListener {

  // Setup the ZooKeeper cache.
  this.instances.getListenable.addListener(this)
  this.instances.start()

  override def execute(
    transaction: thrift.Transaction,
    backoffs: Seq[FiniteDuration]
  ): Try[thrift.Literal] = {
    // Avoids race by caching the available clients.
    val current = this.clients.values.toSeq
    if (current.isEmpty) {
      // If there are no available clients, then throw an error.
      Failure(new IndexOutOfBoundsException("No available servers."))
    } else {
      // Avoid race by synchronizing execution on the randomized client.
      val client = current(Random.nextInt(current.length))
      client.synchronized(client.execute(transaction, backoffs))
    }
  }

  override def close(): Unit = {
    // Avoid race by closing the cache first.
    this.instances.close()
    this.clients.values.foreach(_.close)
  }

  override def childEvent(
    curator: CuratorFramework,
    event: PathChildrenCacheEvent
  ): Unit = event.getType match {
    case PathChildrenCacheEvent.Type.CHILD_ADDED | PathChildrenCacheEvent.Type.CHILD_UPDATED =>
      this.clients += event.getData.getPath -> Connection(Address(event.getData.getData))
    case PathChildrenCacheEvent.Type.CHILD_REMOVED =>
      this.clients.remove(event.getData.getPath) collect { case x: Closeable => x.close() }
    case _ =>
  }

}

object Cluster {

  /**
   * Constructs a Cluster backed by the specified Registry.
   *
   * @param registry Underlying Registry.
   * @return Registry-backed Cluster.
   */
  def apply(registry: Registry): Cluster =
    Cluster(registry.curator)

  /**
   * Constructs a Cluster backed by the specified Curator client.
   *
   * @param curator Zookeeper client.
   * @return Curator-backed Cluster.
   */
  def apply(curator: CuratorFramework): Cluster = {
    val namespace = "/" + curator.getNamespace
    val instances = new PathChildrenCache(curator, namespace, true)
    Cluster(instances, TrieMap.empty[String, Client])
  }

}
