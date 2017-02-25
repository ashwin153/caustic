package com.schema.memcached

import com.schema.core.SnapshotTest
import org.junit.runner.RunWith
import org.scalatest.Outcome
import org.scalatest.junit.JUnitRunner
import scala.concurrent.duration._
import shade.memcached.{Configuration, Memcached}
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class MemcachedSnapshotTest extends SnapshotTest {

  override protected def withFixture(test: OneArgTest): Outcome = {
    // Execution requires a running memcached installation on port 11211 (memcached -p 11211).
    val memcached = Memcached(Configuration("127.0.0.1:11211"))
    val outcome = test(MemcachedSnapshot.empty(memcached, 10 seconds))
    outcome
  }

}
