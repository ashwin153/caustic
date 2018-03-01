package caustic.runtime

import caustic.runtime
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.mockito.internal.stubbing.answers.CallsRealMethods
import org.mockito.invocation.InvocationOnMock
import org.scalatest.{FunSuite, Matchers, Outcome, fixture}
import org.scalatest.junit.JUnitRunner
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import java.util.concurrent.{CountDownLatch, TimeUnit}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

@RunWith(classOf[JUnitRunner])
class RuntimeTest extends FunSuite with MockitoSugar with Matchers {

//  test("Execute is strongly consistent.") {
//    // Get on an unknown key returns the empty string.
//    whenReady(db.execute(read(text("x"))))(_ shouldEqual None)
//
//    // Get on a inserted key returns the inserted value.
//    whenReady(db.execute(write(text("x"), real(1))))(_ shouldEqual real(1))
//    whenReady(db.execute(read(text("x"))))(_ shouldEqual real(1))
//
//    // Get on a modified key within a transaction returns the modified value.
//    whenReady(db.execute(cons(write(text("x"), real(2)), read(text("x")))))(_ shouldEqual real(2))
//  }
//
//  test("Execute detects conflicts.") {
//    // Spy the underlying database to insert latches inside its put method that will enable
//    // deterministic injection of race conditions into transaction execution.
//    val ready = new CountDownLatch(1)
//    val block = new CountDownLatch(1)
//    val fake = spy[Database](db)
//
//    doAnswer(new CallsRealMethods {
//      override def answer(invocation: InvocationOnMock): Object = {
//        ready.countDown()
//        assert(block.await(10, TimeUnit.SECONDS))
//        super.answer(invocation)
//      }
//    }).when(fake).cas(Map("x" -> 0L), Map.empty)(global)
//
//    // Construct a read-only transaction and wait for it to reach the faked 'put' method. Then
//    // construct a second transaction that updates the value of the field and unblocks the first
//    // transaction after it completes execution. This introduces a conflict that should cause the
//    // first transaction to fail.
//    val exec = fake.execute(read(text("x")))
//    assert(ready.await(10, TimeUnit.SECONDS))
//    fake.execute(write(text("x"), text("foo"))).onComplete(_ => block.countDown())
//    whenReady(exec.failed)(_ shouldBe an [Exception])
//    whenReady(db.get(Set("x")))(_ should contain theSameElementsAs Seq("x" -> Revision(1L, text("foo"))))
//  }
//
//  test("Execute is thread-safe.") {
//    // Construct a transaction that increments a counter.
//    val key = text("x")
//    val inc = write(key, add(real(1), branch(runtime.equal(read(key), None), real(0), read(key))))
//
//    // Concurrently execute the transaction and count the total successes.
//    val tasks = Future.sequence(Seq.fill(250)(db.execute(inc).map(_ => 1).fallbackTo(Future(0))))
//    val total = Await.result(tasks, 30 seconds).sum
//
//    // Verify that the number of increments matches the number of successful transactions.
//    whenReady(db.get(Set("x")))(_ should contain theSameElementsAs Seq("x" -> Revision(total, real(total))))
//  }
//
//  test("Execute maintains mutable state.") {
//    // Verifies that local variables and loops are properly handled by the database execution logic.
//    whenReady(db.execute(cons(
//      store(text("i"), real(0)),
//      cons(
//        repeat(
//          less(load(text("i")), real(3)),
//          store(text("i"), add(load(text("i")), real(1)))
//        ),
//        load(text("i"))
//      )
//    )))(_ shouldEqual real(3))
//  }

}
