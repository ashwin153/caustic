package caustic.runtime.service

import org.apache.curator.framework.CuratorFramework

/**
 *
 * @param curator
 * @param path
 */
case class Registry(
  curator: CuratorFramework,
  path: String
)