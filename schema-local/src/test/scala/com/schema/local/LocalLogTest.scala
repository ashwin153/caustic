package com.schema.local

import org.junit.runner.RunWith
import org.scalatest.FutureOutcome
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LocalLogTest extends LogTest {

  override def withFixture(test: OneArgAsyncTest): FutureOutcome = test(LocalLog.empty)

}
