package com.schema.redis

import com.schema.core.Snapshot
import redis.RedisClient
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 *
 * @param redis
 * @param timeout
 */
class RedisSnapshot(redis: RedisClient, timeout: Duration) extends Snapshot {

  override def get(key: String): Option[Any] =
    Await.result(this.redis.get(key), this.timeout)

  override def +=(kv: (String, Any)): Unit =
    Await.result(this.redis.publish(kv._1, kv._2), this.timeout)

  override def -=(key: String): Unit =
    Await.result(this.redis.del(key), this.timeout)

}
