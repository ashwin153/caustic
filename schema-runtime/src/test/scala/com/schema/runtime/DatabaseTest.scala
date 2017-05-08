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
    whenReady(db.execute(literal(0) == literal(0.0)))(_ shouldEqual Literal.True.toString)
    whenReady(db.execute(literal(0) != literal(1)))(_ shouldEqual Literal.True.toString)
    whenReady(db.execute(literal(0) <= literal(0)))(_ shouldEqual Literal.True.toString)
    whenReady(db.execute(literal(0) <  literal(1)))(_ shouldEqual Literal.True.toString)
    whenReady(db.execute(literal(0) >= literal(0)))(_ shouldEqual Literal.True.toString)
    whenReady(db.execute(literal(1) >  literal(0)))(_ shouldEqual Literal.True.toString)
    whenReady(db.execute(literal(0) min literal(1)))(_ shouldEqual Literal.Zero.toString)
    whenReady(db.execute(literal(0) max literal(1)))(_ shouldEqual Literal.One.toString)

    // Logical operations.
    whenReady(db.execute(Literal.False && Literal.True))(_ shouldEqual Literal.False.toString)
    whenReady(db.execute(Literal.True  && Literal.True))(_ shouldEqual Literal.True.toString)
    whenReady(db.execute(Literal.False || Literal.True))(_ shouldEqual Literal.True.toString)
    whenReady(db.execute(!Literal.True))(_ shouldEqual Literal.False.toString)

    // Arithmetic operations.
    whenReady(db.execute(literal(6) + literal(9)))(_ shouldEqual literal(15).toString)
    whenReady(db.execute(literal(9) - literal(6)))(_ shouldEqual literal(3).toString)
    whenReady(db.execute(literal(2) * literal(3)))(_ shouldEqual literal(6).toString)
    whenReady(db.execute(literal(5) / literal(2)))(_ shouldEqual literal(2.5).toString)
    whenReady(db.execute(literal(5) % literal(2)))(_ shouldEqual Literal.One.toString)
    whenReady(db.execute(pow(literal(5), literal(2))))(_ shouldEqual literal(25).toString)
    whenReady(db.execute(log(literal(math.exp(2)))))(_ shouldEqual literal(2).toString)
    whenReady(db.execute(sin(literal(0.0))))(_ shouldEqual literal(0).toString)
    whenReady(db.execute(cos(literal(0.0))))(_ shouldEqual literal(1).toString)
    whenReady(db.execute(ceil(literal(1.0))))(_ shouldEqual literal(1).toString)
    whenReady(db.execute(ceil(literal(1.5))))(_ shouldEqual literal(2).toString)
    whenReady(db.execute(floor(literal(1.0))))(_ shouldEqual literal(1).toString)
    whenReady(db.execute(floor(literal(1.5))))(_ shouldEqual literal(1).toString)
    whenReady(db.execute(round(literal(1.5))))(_ shouldEqual literal(2).toString)
    whenReady(db.execute(floor(literal(1.4))))(_ shouldEqual literal(1).toString)
    whenReady(db.execute(abs(literal(-1))))(_ shouldEqual literal(1).toString)
    whenReady(db.execute(abs(literal(+1))))(_ shouldEqual literal(1).toString)

    // String operations.
    whenReady(db.execute(schema.length("Hello")))(_ shouldEqual literal(5).toString)
    whenReady(db.execute(schema.slice("Hello", 1, 3)))(_ shouldEqual "el")
    whenReady(db.execute(schema.concat("A", "bc")))(_ shouldEqual "Abc")
    whenReady(db.execute(schema.matches("a41i3", "[a-z1-4]+")))(_ shouldEqual Literal.True.toString)
    whenReady(db.execute(schema.contains("abc", "bc")))(_ shouldEqual Literal.True.toString)
    whenReady(db.execute(schema.contains("abc", "de")))(_ shouldEqual Literal.False.toString)
  }

}
