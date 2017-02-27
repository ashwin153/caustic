package com.schema.redis

import com.schema.objects.Schema
import com.schema.objects.transactions._
import scala.concurrent.Future

/**
 * A transactionally modifiable Redis snapshot. Enables the convenient syntactic sugar of
 * [[Schema]] transactions for any Redis snapshot.
 *
 * @param snapshot Underlying redis snapshot.
 */
case class RedisManager(snapshot: RedisSnapshot) extends Manager(snapshot) {

  override def commit(instructions: Map[String, Instruction]): Future[Unit] = {
    val txn = snapshot.redis.transaction()
    instructions foreach {
      case (key, Read) => txn.watch(key)
      case (key, Upsert(value)) => txn.set(key, value)
      case (key, Delete) => txn.del(key)
    }
    txn.exec().map(_ => Unit)
  }

}
