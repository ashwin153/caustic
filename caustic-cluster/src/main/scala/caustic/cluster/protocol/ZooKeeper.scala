package caustic.cluster
package protocol

import caustic.cluster

import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.framework.recipes.cache.PathChildrenCache
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type
import org.apache.curator.framework.state.ConnectionState
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.zookeeper.CreateMode

import scala.collection.mutable
import scala.concurrent.duration._

object ZooKeeper {

  /**
   * A ZooKeeper-managed [[Cluster]]. When an instance joins or leaves a [[ZooKeeper.Cluster]], the
   * [[ZooKeeper.Cluster]] creates or deletes the the instance in ZooKeeper. Instances are
   * automatically deleted during disruptions in ZooKeeper connectivity, because they are created
   * as ephemeral sequential nodes, and they are automatically recreated when connectivity is
   * restored. Each [[ZooKeeper.Cluster]] watches for when instances are created and deleted in
   * ZooKeeper and updates its own view of the cluster accordingly.
   *
   * @param curator An un-started [[CuratorFramework]].
   * @param created Instances created by this [[Dynamic]] cluster.
   * @param awareOf Instances known to this [[Dynamic]] cluster.
   */
  case class Cluster[C](
    service: Service[C],
    curator: CuratorFramework,
    created: mutable.Map[Address, String] = mutable.Map.empty,
    awareOf: mutable.Map[String, Address] = mutable.Map.empty
  ) extends cluster.Cluster[C] {

    // Re-register all created addresses after disruptions in ZooKeeper connectivity.
    this.curator.getConnectionStateListenable.addListener((_, s) =>
      if (s == ConnectionState.CONNECTED || s == ConnectionState.RECONNECTED) {
        this.created.keys.foreach(join)
      }
    )

    // Construct a ZooKeeper cache to monitor instance changes.
    private val cache = new PathChildrenCache(this.curator, "/" + this.curator.getNamespace, false)
    this.cache.getListenable.addListener((_, e) =>
      if (e.getType == Type.CHILD_ADDED || e.getType == Type.CHILD_UPDATED) {
        val address = Address(e.getData.getData)
        this.awareOf += e.getData.getPath -> address
      } else if (e.getType == Type.CHILD_REMOVED) {
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

    override def members: Set[Address] = {
      this.awareOf.values.toSet
    }

    override def join(instance: Address): Unit = {
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

    override def leave(instance: Address): Unit = {
      this.created.remove(instance) foreach { p =>
        this.curator.blockUntilConnected()
        this.curator.delete().forPath(p)
      }
    }

  }

  object Cluster {

    /**
     * A [[ZooKeeper.Cluster]] configuration.
     *
     * @param zookeeper Connection string. (eg. "localhost:3192,localhost:2811")
     * @param namespace Directory in which instances are registred.
     * @param connectionTimeout ZooKeeper connection timeout.
     * @param sessionTimeout ZooKeeper session timeout.
     */
    case class Config(
      zookeeper: String = "localhost:2811",
      namespace: String = "services",
      connectionTimeout: Duration = 15 seconds,
      sessionTimeout: Duration = 60 seconds
    )

    /**
     * Constructs a [[ZooKeeper.Cluster]] from the provided configuration.
     *
     * @param config Configuration.
     * @return Configured [[ZooKeeper.Cluster]].
     */
    def apply[C](service: Service[C], config: Config): ZooKeeper.Cluster[C] =
      ZooKeeper.Cluster[C](service, CuratorFrameworkFactory.builder()
        .connectString(config.zookeeper)
        .namespace(config.namespace)
        .retryPolicy(new ExponentialBackoffRetry(1000, 3))
        .connectionTimeoutMs(config.connectionTimeout.toMillis.toInt)
        .sessionTimeoutMs(config.sessionTimeout.toMillis.toInt)
        .build())

  }

}
