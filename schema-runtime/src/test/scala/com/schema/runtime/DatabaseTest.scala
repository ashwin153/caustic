package com.schema.runtime

import com.schema.runtime.local.LocalDatabase
import java.util.concurrent.{CountDownLatch, TimeUnit}
import org.junit.runner.RunWith
import org.scalatest.{Matchers, Outcome, fixture}
import org.scalatest.junit.JUnitRunner
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.internal.stubbing.answers.CallsRealMethods
import org.mockito.invocation.InvocationOnMock
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class DatabaseTest extends fixture.FunSuite with ScalaFutures with MockitoSugar with Matchers {

  type FixtureParam = Database

  def withFixture(test: OneArgTest): Outcome = {
    test(LocalDatabase.empty)
  }

  test("Execute is strongly consistent.") { db =>
    // Get on an unknown key returns the empty string.
    whenReady(db.execute(read("x")))(r => assert(r === ""))

    // Get on a inserted key returns the inserted value.
    whenReady(db.execute(write("x", "1"))) { r => assert(r == "1") }
    whenReady(db.execute(read("x")))(r => assert(r === "1"))

    // Get on a modified key within a transaction returns the modified value.
    whenReady(db.execute(cons(write("x", "2"), read("x"))))(r => assert(r == "2"))
  }

  test("Execute is thread-safe.") { db =>
    // Spy the underlying database to insert latches inside its put method that will enable
    // deterministic injection of race conditions into transaction execution.
    val ready = new CountDownLatch(1)
    val block = new CountDownLatch(1)
    val fake  = spy(db)

    when(fake.put(Map("x" -> 0), Map.empty)(global)).thenAnswer(new CallsRealMethods {
      override def answer(invocation: InvocationOnMock): Object = {
        ready.countDown()
        assert(block.await(10, TimeUnit.SECONDS))
        super.answer(invocation)
      }
    })

    // Construct a read-only transaction and wait for it to reach the faked 'put' method. Then
    // construct a second transaction that updates the value of the field and unblocks the first
    // transaction after it completes execution. This introduces a conflict that should cause the
    // first transaction to fail.
    val exec = fake.execute(read("x"))
    assert(ready.await(10, TimeUnit.SECONDS))
    fake.execute(write("x", "foo")).onComplete(_ => block.countDown())
    whenReady(exec.failed)(_ shouldBe an [Exception])
  }

}
