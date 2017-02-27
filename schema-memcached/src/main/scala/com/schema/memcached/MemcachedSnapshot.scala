package com.schema.memcached

import com.schema.core.Snapshot
import com.schema.memcached.MemcachedSnapshot.ChillCodec
import com.schema.transactions.Instruction
import com.twitter.chill.{KryoPool, ScalaKryoInstantiator}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import shade.memcached.{Codec, Memcached}

/**
 * A memcached-backed snapshot. Memcached snapshots delegate accesses and modifications to an
 * underlying memcached cache. Although memcached snapshots perform worse than the on-heap snapshot
 * because they require an additional serialization step, they are useful for programs with larger
 * snapshot storage requirements.
 *
 * @param memcached Memcached client.
 * @param timeout Memcached request timeout duration.
 * @param codec Memcached value serializer.
 */
class MemcachedSnapshot(
  memcached: Memcached,
  timeout: Duration,
  codec: Codec[Any] = new ChillCodec(ScalaKryoInstantiator.defaultPool)
) extends Snapshot {

  override def get(key: String): Option[Any] =
    Await.result(this.memcached.get(key)(this.codec), this.timeout)

  override def +=(kv: (String, Any)): Unit =
    Await.result(this.memcached.set(kv._1, kv._2, Duration.Inf)(this.codec), this.timeout)

  override def -=(key: String): Unit =
    Await.result(this.memcached.delete(key), this.timeout)

}


object MemcachedSnapshot {

  /**
   * A Memcached [[Codec]] backed by Twitter's Chill, a thin Scala wrapper over the Kryo
   * serialization framework. Kryo has significantly higher performance and greater compression than
   * standard Java serialization.
   *
   * @param kryo Kryo pool.
   */
  class ChillCodec(kryo: KryoPool) extends Codec[Any] {

    override def serialize(value: Any): Array[Byte] =
      this.kryo.toBytesWithClass(value)

    override def deserialize(data: Array[Byte]): Any =
      this.kryo.fromBytes(data)

  }

}