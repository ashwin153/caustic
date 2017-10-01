package caustic.runtime.service

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.state.{ConnectionState, ConnectionStateListener}
import org.apache.zookeeper.CreateMode

/**
 *
 * @param path
 * @param instance
 */
case class Announcer(
  path: String,
  instance: Instance
) extends ConnectionStateListener {

  // Announce the instance as an ephemeral sequential znode.
  override def stateChanged(curator: CuratorFramework, state: ConnectionState): Unit =
    if (state == ConnectionState.CONNECTED || state == ConnectionState.RECONNECTED)
        curator.create().orSetData()
          .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
          .forPath(s"${this.path}/instance", instance.toBytes)

}