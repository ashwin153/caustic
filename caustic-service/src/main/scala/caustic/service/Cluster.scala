package caustic.service

import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.framework.recipes.cache.PathChildrenCache
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type._
import org.apache.curator.framework.state.ConnectionState
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.zookeeper.CreateMode

import java.io.Closeable

import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.{Random, Try}

/**
 * A ZooKeeper-managed collection of instances. Each [[Cluster]] is notified whenever an instance
 * joins and leaves. All instances created by a [[Cluster]] are automatically left on close.
 *
 * @param service [[Service]] implementation.
 * @param curator An un-started [[CuratorFramework]].
 * @param created Instances created by this [[Cluster]].
 * @param awareOf Instances known to the [[Cluster]].
 */
class Cluster[Client](
  service: Service[Client],
  curator: CuratorFramework,
  created: mutable.Map[Address, String] = mutable.Map.empty,
  awareOf: mutable.Map[String, Address] = mutable.Map.empty
) extends Closeable {

  // Re-register all created addresses after disruptions in ZooKeeper connectivity.
  this.curator.getConnectionStateListenable.addListener((_, s) =>
    if (s == ConnectionState.CONNECTED || s == ConnectionState.RECONNECTED) {
      this.created.keys.foreach(join)
    }
  )

  // Construct a ZooKeeper cache to monitor instance changes.
  private val cache = new PathChildrenCache(this.curator, "/" + this.curator.getNamespace, false)
  this.cache.getListenable.addListener((_, e) =>
    if (e.getType == CHILD_ADDED || e.getType == CHILD_UPDATED) {
      val address = Address(e.getData.getData)
      this.awareOf += e.getData.getPath -> address
    } else if (e.getType == CHILD_REMOVED) {
      this.awareOf.remove(e.getData.getPath)
    }
  )

  // Connect to ZooKeeper.
  this.cache.start()
  this.curator.start()

  override def close(): Unit = {
    this.cache.close()
    this.curator.close()
  }

  /**
   * Returns the known members of the [[Cluster]].
   *
   * @return Current members.
   */
  def members: Set[Address] = this.awareOf.values.toSet

  /**
   * Returns the number of members.
   *
   * @return Number of members.
   */
  def size: Int = this.members.size

  /**
   * Adds the instance to the [[Cluster]].
   *
   * @param instance Instance [[Address]].
   */
  def join(instance: Address): Unit = {
    // Remove the instance if it already exists.
    leave(instance)

    // Announce the instance in ZooKeeper.
    this.curator.blockUntilConnected()
    this.created += instance -> this.curator.create()
      .creatingParentContainersIfNeeded()
      .withProtection()
      .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
      .forPath("/instance", instance.toBytes)
  }

  /**
   * Removes the instance from the [[Cluster]].
   *
   * @param instance Intance [[Address]].
   */
  def leave(instance: Address): Unit = {
    this.created.remove(instance) foreach { p =>
      this.curator.blockUntilConnected()
      this.curator.delete().forPath(p)
    }
  }

  /**
   * Performs the request on all members of the [[Cluster]] in parallel and returns their responses.
   *
   * @param request Request to perform.
   * @return Collection of responses.
   */
  def broadcast[Response](request: Client => Response): Seq[Try[Response]] = {
    val client = this.members.map(service.connect)
    val result = client.par.map(c => Try(request(c))).seq.toSeq
    client.foreach(this.service.disconnect)
    result
  }

  /**
   * Performs the request on all members of the [[Cluster]] in parallel and returns their responses
   * if and only if a majority of the requests were successful.
   *
   * @param request Request to perform.
   * @return Collection of responses.
   */
  def quorum[Response](request: Client => Response): Try[Seq[Response]] = {
    Try(broadcast(request).filter(_.isSuccess).map(_.get)).filter(_.size < this.size / 2 + 1)
  }

  /**
   * Performs the request on a randomly chosen member of the [[Cluster]] and returns the response.
   *
   * @param request Request to perform.
   * @return Collection of responses.
   */
  def random[Response](request: Client => Response): Try[Response] = {
    val client = service.connect(Random.shuffle(this.members).head)
    val result = Try(request(client))
    service.disconnect(client)
    result
  }

}

object Cluster {

  /**
   * A [[Cluster]] configuration.
   *
   * @param zookeeper Connection string. (eg. "localhost:3192,localhost:2811")
   * @param namespace Directory in which instances are registred.
   * @param connectionTimeout ZooKeeper connection timeout.
   * @param sessionTimeout ZooKeeper session timeout.
   */
  case class Config(
    zookeeper: String,
    namespace: String = "services",
    connectionTimeout: Duration = 15 seconds,
    sessionTimeout: Duration = 60 seconds
  )

  /**
   * Constructs a [[Cluster]] from the provided configuration.
   *
   * @param config Configuration.
   * @return Configured [[Cluster]].
   */
  def apply[Client](service: Service[Client], config: Config): Cluster[Client] =
    new Cluster(service, CuratorFrameworkFactory.builder()
      .connectString(config.zookeeper)
      .namespace(config.namespace)
      .retryPolicy(new ExponentialBackoffRetry(1000, 3))
      .connectionTimeoutMs(config.connectionTimeout.toMillis.toInt)
      .sessionTimeoutMs(config.sessionTimeout.toMillis.toInt)
      .build())

}