package caustic.service

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
  paths: mutable.Map[Address, String] = mutable.Map.empty,
  announcers: mutable.Map[Address, ConnectionStateListener] = mutable.Map.empty
) {

  /**
   * A ZooKeeper listener that re-registers an instance address after disruptions in connectivity.
   *
   * @param address Instance location.
   */
  case class Announcer(address: Address) extends ConnectionStateListener {

    override def stateChanged(curator: CuratorFramework, state: ConnectionState): Unit = {
      // Re-register the instance each time the ZooKeeper connection is established.
      if (state == ConnectionState.CONNECTED || state == ConnectionState.RECONNECTED)
        register(address)
    }

  }

  /**
   * Registers the instance in ZooKeeper. A serialized representation of the instance is written to
   * a ephemeral, sequential znode, which ensures that instances are automatically unregistered
   * during ZooKeeper connectivity disruptions. Instances are automatically re-registered whenever
   * ZooKeeper connectivity is resumed.
   *
   * @param address Instance location.
   */
  def register(address: Address): Unit = this.synchronized {
    // Unregister the instance if it already exists.
    unregister(address)

    // Announce the instance in ZooKeeper.
    this.curator.blockUntilConnected()
    this.paths += address -> curator.create()
      .creatingParentContainersIfNeeded()
      .withProtection()
      .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
      .forPath(s"${this.namespace}/instance", address.toBytes)

    // Re-announce the instance every time connection is reestablished.
    val announcer = Announcer(address)
    this.announcers += address -> announcer
    this.curator.getConnectionStateListenable.addListener(announcer)
  }

  /**
   * Unregisters the instance in ZooKeeper. Instances are automatically unregistered during
   * ZooKeeper connectivity disruptions, but instances must still unregister themselves whenever
   * they terminate.
   *
   * @param address Instance location.
   */
  def unregister(address: Address): Unit = this.synchronized {
    if (this.paths.contains(address)) {
      // Remove the instance from ZooKeeper.
      this.curator.blockUntilConnected()
      this.curator.delete().forPath(this.paths(address))
      this.curator.getConnectionStateListenable.removeListener(this.announcers(address))

      // Delete the instance.
      this.paths -= address
      this.announcers -= address
    }
  }

}
