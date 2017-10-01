package caustic.runtime
package service

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.state.{ConnectionState, ConnectionStateListener}
import org.apache.zookeeper.CreateMode

import scala.collection.mutable

/**
 * An instance registry. Thread-safe.
 *
 * @param curator ZooKeeper connection.
 * @param namespace ZooKeeper base path.
 * @param paths Instance ZooKeeper paths.
 * @param announcers Instance announcers.
 */
case class Registry(
  curator: CuratorFramework,
  namespace: String,
  paths: mutable.Map[Instance, String],
  announcers: mutable.Map[Instance, ConnectionStateListener]
) {

  /**
   * A listener that re-registers an instance after disruptions in ZooKeeper connectivity.
   *
   * @param instance Server instance.
   */
  case class Announcer(instance: Instance) extends ConnectionStateListener {

    override def stateChanged(curator: CuratorFramework, state: ConnectionState): Unit = {
      // Re-register the instance each time the ZooKeeper connection is established.
      if (state == ConnectionState.CONNECTED || state == ConnectionState.RECONNECTED)
        register(instance)
    }

  }

  /**
   * Registers the instance in ZooKeeper. A serialized representation of the instance is written to
   * a ephemeral, sequential znode, which ensures that instances are automatically unregistered
   * during ZooKeeper connectivity disruptions. Instances are automatically re-registered whenever
   * ZooKeeper connectivity is resumed.
   *
   * @param instance Instance to register.
   */
  def register(instance: Instance): Unit = this.synchronized {
    // Unregister the instance if it already exists.
    unregister(instance)

    // Announce the instance in ZooKeeper.
    this.paths += instance -> curator.create()
      .creatingParentContainersIfNeeded()
      .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
      .forPath(s"${this.namespace}/instance", instance.toBytes)

    // Re-announce the instance every time connection is reestablished.
    val announcer = Announcer(instance)
    this.announcers += instance -> announcer
    this.curator.getConnectionStateListenable.addListener(announcer)
  }

  /**
   * Unregisters the instance in ZooKeeper. Instances are automatically unregistered during
   * ZooKeeper connectivity disruptions, but instances must still unregister themselves whenever
   * they terminate.
   *
   * @param instance Instance to unregister.
   */
  def unregister(instance: Instance): Unit = this.synchronized {
    if (this.paths.contains(instance)) {
      // Remove the instance from ZooKeeper.
      curator.delete().forPath(this.paths(instance))
      curator.getConnectionStateListenable.removeListener(this.announcers(instance))

      // Delete the instance.
      this.paths -= instance
      this.announcers -= instance
    }
  }

}

object Registry {

  /**
   * Constructs a registry at the specified namespace in ZooKeeper.
   *
   * @param curator ZooKeeper connection.
   * @param namespace ZooKeeper base path.
   * @return Default registry.
   */
  def apply(curator: CuratorFramework, namespace: String): Registry =
    Registry(curator, namespace, mutable.Map.empty, mutable.Map.empty)

}