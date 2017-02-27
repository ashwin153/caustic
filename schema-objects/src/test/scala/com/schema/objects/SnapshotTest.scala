package com.schema.objects

import org.junit.runner.RunWith
import org.scalatest.fixture
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
trait SnapshotTest extends fixture.FunSuite {

  override type FixtureParam = Snapshot

  test("Get, +=, and -= should be strongly consistent.") { snapshot =>
    assert(snapshot.get("x").isEmpty)
    snapshot += "x" -> 0
    assert(snapshot.get("x").contains(0))
    snapshot += "x" -> 1
    assert(snapshot.get("x").contains(1))
    snapshot -= "x"
    assert(snapshot.get("x").isEmpty)
  }

}
