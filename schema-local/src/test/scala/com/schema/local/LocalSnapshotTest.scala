package com.schema.local

import com.schema.core.SnapshotTest
import org.junit.runner.RunWith
import org.scalatest.Outcome
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LocalSnapshotTest extends SnapshotTest {

  override protected def withFixture(test: OneArgTest): Outcome = test(LocalSnapshot.empty)

}
