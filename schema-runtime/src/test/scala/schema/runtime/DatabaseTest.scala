package schema.runtime

import schema._
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
abstract class DatabaseTest extends fixture.FunSuite
  with ScalaFutures
  with MockitoSugar
  with Matchers {

  type FixtureParam = Database

  def withFixture(test: OneArgTest): Outcome

  test("Execute is strongly consistent.") { db =>
    // Get on an unknown key returns the empty string.
    whenReady(db.execute(read(literal("x"))))(_ shouldEqual "")

    // Get on a inserted key returns the inserted value.
    whenReady(db.execute(write(literal("x"), literal("1"))))(_ shouldEqual "1")
    whenReady(db.execute(read(literal("x"))))(_ shouldEqual "1")

    // Get on a modified key within a transaction returns the modified value.
    whenReady(db.execute(cons(write(literal("x"), literal("2")), read(literal("x")))))(_ shouldEqual "2")
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
    val exec = fake.execute(read(literal("x")))
    assert(ready.await(10, TimeUnit.SECONDS))
    fake.execute(write(literal("x"), literal("foo"))).onComplete(_ => block.countDown())
    whenReady(exec.failed)(_ shouldBe an [Exception])
  }

  test("Execute correctly performs operations.") { db =>
    // Logical operations.
    whenReady(db.execute(runtime.equal(literal(0), literal(0.0))))(_ shouldEqual Literal.True.value)
    whenReady(db.execute(runtime.not(runtime.equal(literal(0), literal(1)))))(_ shouldEqual Literal.True.value)
    whenReady(db.execute(less(literal(0), literal(1))))(_ shouldEqual Literal.True.value)

    // Logical operations.
    whenReady(db.execute(and(Literal.False, Literal.True)))(_ shouldEqual Literal.False.value)
    whenReady(db.execute(and(Literal.True, Literal.True)))(_ shouldEqual Literal.True.value)
    whenReady(db.execute(or(Literal.False, Literal.True)))(_ shouldEqual Literal.True.value)
    whenReady(db.execute(runtime.not(Literal.True)))(_ shouldEqual Literal.False.value)

    // Arithmetic operations.
    whenReady(db.execute(add(literal(6), literal(9))))(_ shouldEqual "15.0")
    whenReady(db.execute(sub(literal(9), literal(6))))(_ shouldEqual "3.0")
    whenReady(db.execute(mul(literal(2), literal(3))))(_ shouldEqual "6.0")
    whenReady(db.execute(div(literal(5), literal(2))))(_ shouldEqual "2.5")
    whenReady(db.execute(mod(literal(5), literal(2))))(_ shouldEqual Literal.One.value)
    whenReady(db.execute(pow(literal(5), literal(2))))(_ shouldEqual "25.0")
    whenReady(db.execute(log(literal(math.exp(2)))))(_ shouldEqual Literal.Two.value)
    whenReady(db.execute(sin(literal(0.0))))(_ shouldEqual Literal.Zero.value)
    whenReady(db.execute(cos(literal(0.0))))(_ shouldEqual Literal.One.value)
    whenReady(db.execute(floor(literal(1.0))))(_ shouldEqual Literal.One.value)
    whenReady(db.execute(floor(literal(1.5))))(_ shouldEqual Literal.One.value)
    whenReady(db.execute(floor(literal(1.4))))(_ shouldEqual Literal.One.value)

    // String operations.
    whenReady(db.execute(runtime.length(literal("Hello"))))(_ shouldEqual "5.0")
    whenReady(db.execute(runtime.slice(literal("Hello"), literal(1), literal(3))))(_ shouldEqual "el")
    whenReady(db.execute(runtime.concat(literal("A"), literal("bc"))))(_ shouldEqual "Abc")
    whenReady(db.execute(runtime.matches(literal("a41i3"), literal("[a-z1-4]+"))))(_ shouldEqual Literal.True.value)
    whenReady(db.execute(runtime.contains(literal("abc"), literal("bc"))))(_ shouldEqual Literal.True.value)
    whenReady(db.execute(runtime.contains(literal("abc"), literal("de"))))(_ shouldEqual Literal.False.value)

    // Loop operations.
    whenReady(db.execute(cons(
      store(literal("$i"), literal(0)),
      cons(
        repeat(
          less(load(literal("$i")), literal(3)),
          store(literal("$i"), add(load(literal("$i")), literal(1)))),
        load(literal("$i"))))
    ))(_ shouldEqual "3.0")
  }

}
