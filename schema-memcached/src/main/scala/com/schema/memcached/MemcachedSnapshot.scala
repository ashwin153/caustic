package com.schema.memcached

import com.schema.core.Snapshot
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import shade.memcached.Memcached

/**
 *
 * @param memcached
 * @param timeout
 */
class MemcachedSnapshot(memcached: Memcached, timeout: Duration) extends Snapshot {

  override def get(key: String): Option[Any] =
    Await.result(this.memcached.get(key), this.timeout)

  override def +=(kv: (String, Any)): Unit =
    Await.result(this.memcached.set(kv._1, kv._2, Duration.Inf), this.timeout)

  override def -=(key: String): Unit =
    Await.result(this.memcached.delete(key), this.timeout)

}
