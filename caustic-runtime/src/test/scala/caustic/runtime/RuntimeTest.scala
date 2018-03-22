package caustic.runtime

import caustic.{runtime => caustic}

import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.mockito.internal.stubbing.answers.CallsRealMethods
import org.mockito.invocation.InvocationOnMock
import org.scalatest.{FunSuite, Matchers}
import org.scalatest.junit.JUnitRunner
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time._

import java.util.concurrent.{CountDownLatch, TimeUnit}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class RuntimeTest extends FunSuite with MockitoSugar with ScalaFutures with Matchers {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(
    timeout = Span(5, Seconds),
    interval = Span(100, Millis)
  )

  test("Execute is strongly consistent.") {
    val runtime = Runtime(Database.Local())

    // Get on an unknown key returns a null value.
    runtime.execute(read(text("x"))) shouldBe Success(Null)

    // Get on a inserted key returns the inserted value.
    runtime.execute(write(text("x"), real(1))) shouldBe Success(Null)
    runtime.execute(read(text("x"))) shouldBe Success(real(1))

    // Get on a modified key within a transaction returns the modified value.
    runtime.execute(cons(write(text("x"), real(2)), read(text("x")))) shouldBe Success(real(2))
  }

  test("Execute detects conflicts.") {
    // Spy the underlying database to insert latches inside its put method that will enable
    // deterministic injection of race conditions into transaction execution.
    val fake = spy(Database.Local())
    val runtime = Runtime(fake)
    val ready = new CountDownLatch(1)
    val block = new CountDownLatch(1)

    doAnswer(new CallsRealMethods {
      override def answer(invocation: InvocationOnMock): Object = {
        ready.countDown()
        assert(block.await(1, TimeUnit.SECONDS))
        super.answer(invocation)
      }
    }).when(fake).cas(Map("x" -> 0L), Map.empty)

    // Construct a read-only transaction and wait for it to reach the faked 'put' method. Then
    // construct a second transaction that updates the value of the field and unblocks the first
    // transaction after it completes execution. This introduces a conflict that should cause the
    // first transaction to fail.
    val exec = Future(runtime.execute(read(text("x"))).get)
    assert(ready.await(100, TimeUnit.MILLISECONDS))
    runtime.execute(write(text("x"), text("foo"))) shouldBe Success(Null)
    block.countDown()
    whenReady(exec.failed)(_ shouldBe an [Exception])
    runtime.execute(read(text("x"))) shouldBe Success(text("foo"))
  }

  test("Execute is thread-safe.") {
    // Construct a transaction that increments a counter.
    val runtime = Runtime(Database.Local())
    val key = text("x")
    val inc = write(key, add(real(1), branch(caustic.equal(read(key), Null), real(0), read(key))))

    // Concurrently execute the transaction and count the total successes.
    val tasks = Future.sequence(Seq.fill(99)(Future(runtime.execute(inc).map(_ => 1).getOrElse(0))))
    val total = Await.result(tasks, 30 seconds).sum

    // Verify that the number of increments matches the number of successful transactions.
    runtime.execute(read(key)) shouldBe Success(real(total))
  }

  test("Execute maintains mutable state.") {
    // Verifies that local variables and loops are properly handled by the database execution logic.
    Runtime(Database.Local()) execute {
      cons(
        store(text("i"), real(0)),
        cons(
          repeat(
            less(load(text("i")), real(3)),
            store(text("i"), add(load(text("i")), real(1)))
          ),
          load(text("i"))
        )
      )
    } shouldBe Success(real(3))
  }

}
