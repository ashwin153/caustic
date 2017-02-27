package com.schema.redis

import akka.util.ByteString
import com.schema.objects.Snapshot
import com.schema.redis.RedisSnapshot.ChillFormatter
import com.schema.objects.transactions.{Delete, Instruction, Read, Upsert}
import redis.{ByteStringFormatter, RedisClient}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import com.twitter.chill.{KryoPool, ScalaKryoInstantiator}

/**
 * A redis-backed snapshot. Redis snapshots delegate accesses and modifications to an underlying
 * redis cache. Although redis snapshots perform worse than the on-heap snapshot because they
 * require an additional serialization step, they are useful for programs with larger snapshot
 * storage requirements.
 *
 * @param redis Redis client.
 * @param timeout Redis request timeout duration.
 * @param formatter Redis data formatter.
 */
case class RedisSnapshot(
  redis: RedisClient,
  timeout: Duration,
  formatter: ByteStringFormatter[Any] = ChillFormatter(ScalaKryoInstantiator.defaultPool)
) extends Snapshot {

  override def get(key: String): Option[Any] =
    Await.result(this.redis.get(key)(this.formatter), this.timeout)

  override def +=(kv: (String, Any)): Unit =
    Await.result(this.redis.set(kv._1, kv._2)(this.formatter), this.timeout)

  override def -=(key: String): Unit =
    Await.result(this.redis.del(key), this.timeout)

}

object RedisSnapshot {

  /**
   * A Redis [[ByteStringFormatter]] backed by Twitter's Chill, a thin Scala wrapper over the Kryo
   * serialization framework. Kryo has significantly higher performance and greater compression over
   * standard Java serialization.
   *
   * @param kryo Kryo pool.
   */
  case class ChillFormatter(kryo: KryoPool) extends ByteStringFormatter[Any] {

    override def serialize(data: Any): ByteString =
      ByteString(this.kryo.toBytesWithClass(data))

    def deserialize(bs: ByteString): Any =
      this.kryo.fromBytes(bs.toArray)

  }

}
