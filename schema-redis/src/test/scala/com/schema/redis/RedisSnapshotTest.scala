package com.schema.redis

import com.schema.core.SnapshotTest
import org.junit.runner.RunWith
import org.scalatest.Outcome
import org.scalatest.junit.JUnitRunner
import scala.concurrent.duration._
import redis.RedisClient

@RunWith(classOf[JUnitRunner])
class RedisSnapshotTest extends SnapshotTest {

  override protected def withFixture(test: OneArgTest): Outcome = {
    // Execution requires a running redis installation (redis-server).
    implicit val akkaSystem = akka.actor.ActorSystem()
    val outcome = test(RedisSnapshot.empty(RedisClient(), 10 seconds))
    akkaSystem.terminate()
    outcome
  }

}
