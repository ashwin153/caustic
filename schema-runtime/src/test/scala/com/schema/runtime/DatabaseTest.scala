package com.schema.runtime

import com.schema.{runtime => schema}
import com.schema.runtime.local.SynchronizedDatabase
import java.util.concurrent.{CountDownLatch, TimeUnit}
import org.junit.runner.RunWith
import org.scalatest.{Matchers, Outcome, fixture}
import org.scalatest.junit.JUnitRunner
import org.scalatest.concurrent.ScalaFutures
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.mockito.internal.stubbing.answers.CallsRealMethods
import org.mockito.invocation.InvocationOnMock
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class DatabaseTest extends fixture.FunSuite with ScalaFutures with MockitoSugar with Matchers {

  type FixtureParam = Database

  def withFixture(test: OneArgTest): Outcome = {
    test(SynchronizedDatabase.empty)
  }

  test("Execute is strongly consistent.") { db =>
    // Get on an unknown key returns the empty string.
    whenReady(db.execute(read("x")))(_ shouldEqual "")

    // Get on a inserted key returns the inserted value.
    whenReady(db.execute(write("x", "1")))(_ shouldEqual "1")
    whenReady(db.execute(read("x")))(_ shouldEqual "1")

    // Get on a modified key within a transaction returns the modified value.
    whenReady(db.execute(cons(write("x", "2"), read("x"))))(_ shouldEqual "2")
  }

  test("Execute is thread-safe.") { db =>
    // Spy the underlying database to insert latches inside its put method that will enable
    // deterministic injection of race conditions into transaction execution.
    val ready = new CountDownLatch(1)
    val block = new CountDownLatch(1)
    val fake = spy(db)

    when(fake.put(Map("x" -> 0L), Map.empty)(global)).thenAnswer(new CallsRealMethods {
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

  test("Execute correctly performs operations.") { db =>
    // Logical operations.
    whenReady(db.execute(schema.equal(0, 0.0)))(_ shouldEqual Literal.True.value)
    whenReady(db.execute(schema.not(schema.equal(0, 1))))(_ shouldEqual Literal.True.value)
    whenReady(db.execute(less(0, 1)))(_ shouldEqual Literal.True.value)

    // Logical operations.
    whenReady(db.execute(and(Literal.False, Literal.True)))(_ shouldEqual Literal.False.value)
    whenReady(db.execute(and(Literal.True, Literal.True)))(_ shouldEqual Literal.True.value)
    whenReady(db.execute(or(Literal.False, Literal.True)))(_ shouldEqual Literal.True.value)
    whenReady(db.execute(schema.not(Literal.True)))(_ shouldEqual Literal.False.value)

    // Arithmetic operations.
    whenReady(db.execute(add(6, 9)))(_ shouldEqual "15.0")
    whenReady(db.execute(sub(9, 6)))(_ shouldEqual "3.0")
    whenReady(db.execute(mul(2, 3)))(_ shouldEqual "6.0")
    whenReady(db.execute(div(5, 2)))(_ shouldEqual "2.5")
    whenReady(db.execute(mod(5, 2)))(_ shouldEqual Literal.One.value)
    whenReady(db.execute(pow(5, 2)))(_ shouldEqual "25.0")
    whenReady(db.execute(log(math.exp(2))))(_ shouldEqual Literal.Two.value)
    whenReady(db.execute(sin(0.0)))(_ shouldEqual Literal.Zero.value)
    whenReady(db.execute(cos(0.0)))(_ shouldEqual Literal.One.value)
    whenReady(db.execute(floor(1.0)))(_ shouldEqual Literal.One.value)
    whenReady(db.execute(floor(1.5)))(_ shouldEqual Literal.One.value)
    whenReady(db.execute(floor(1.4)))(_ shouldEqual Literal.One.value)

    // String operations.
    whenReady(db.execute(schema.length("Hello")))(_ shouldEqual "5.0")
    whenReady(db.execute(schema.slice("Hello", 1, 3)))(_ shouldEqual "el")
    whenReady(db.execute(schema.concat("A", "bc")))(_ shouldEqual "Abc")
    whenReady(db.execute(schema.matches("a41i3", "[a-z1-4]+")))(_ shouldEqual Literal.True.value)
    whenReady(db.execute(schema.contains("abc", "bc")))(_ shouldEqual Literal.True.value)
    whenReady(db.execute(schema.contains("abc", "de")))(_ shouldEqual Literal.False.value)

    // Loop operations.
    whenReady(db.execute(cons(store("$i", 0), cons(repeat(less(load("$i"), 3), store("$i", add(load("$i"), 1))), load("$i")))))(_ shouldEqual "3.0")
  }

}
