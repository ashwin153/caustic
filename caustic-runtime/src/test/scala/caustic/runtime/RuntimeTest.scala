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
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class RuntimeTest extends FunSuite with MockitoSugar with ScalaFutures with Matchers {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(
    timeout = Span(5, Seconds),
    interval = Span(100, Millis)
  )

  test("Execute is strongly consistent.") {
    val runtime = Runtime(Volume.Memory())

    // Get on an unknown key returns a null value.
    runtime.execute(read("x")) shouldBe Success(Null)

    // Get on a inserted key returns the inserted value.
    runtime.execute(write("x", 1)) shouldBe Success(Null)
    runtime.execute(read("x")) shouldBe Success(real(1))

    // Get on a modified key within a transaction returns the modified value.
    runtime.execute(cons(write("x", 2), read("x"))) shouldBe Success(real(2))
  }

  test("Execute is thread-safe.") {
    val runtime = Runtime(Volume.Memory())

    // Construct a transaction that increments a counter.
    val inc = write("x", add(1, branch(caustic.equal(read("x"), Null), 0, read("x"))))

    // Concurrently execute the transaction and count the total successes.
    val total = new AtomicInteger(0)
    val tasks = Seq.fill(8)(new Thread(() =>
      Seq.fill(10000)(runtime.execute(inc).foreach(_ => total.getAndIncrement()))
    ))

    // Verify that the number of increments matches the number of successful transactions.
    tasks.foreach(_.start())
    tasks.foreach(_.join())
    runtime.execute(read("x")) shouldBe Success(real(total.get()))
  }

  test("Execute maintains mutable state.") {
    val runtime = Runtime(Volume.Memory())

    // Verifies that local variables are correctly updated.
    runtime execute {
      cons(
        store("i", 0),
        cons(repeat(less(load("i"), 3), store("i", add(load("i"), 1))), load("i"))
      )
    } shouldBe Success(real(3))
  }

  test("Execute detects conflicts.") {
    // Spy the underlying database to insert latches inside its put method that will enable
    // deterministic injection of race conditions into transaction execution.
    val fake = spy(Volume.Memory())
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
    val exec = Future(runtime.execute(read("x")).get)
    assert(ready.await(100, TimeUnit.MILLISECONDS))
    runtime.execute(write("x", "foo")) shouldBe Success(Null)
    block.countDown()

    // Verify that the first transaction fails, but the second succeeds.
    whenReady(exec.failed)(_ shouldBe an [Exception])
    runtime.execute(read("x")) shouldBe Success(text("foo"))
  }

}
