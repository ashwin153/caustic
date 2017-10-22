package caustic.runtime

import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.mockito.internal.stubbing.answers.CallsRealMethods
import org.mockito.invocation.InvocationOnMock
import org.scalatest.{Matchers, Outcome, fixture}
import org.scalatest.junit.JUnitRunner
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}

import java.util.concurrent.{CountDownLatch, TimeUnit}
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
trait DatabaseTest extends fixture.FunSuite
  with ScalaFutures
  with MockitoSugar
  with Matchers {

  type FixtureParam = Database

  implicit val defaultPatience = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(100, Millis)
  )

  def withFixture(test: OneArgTest): Outcome

  test("Execute is strongly consistent.") { db =>
    // Get on an unknown key returns the empty string.
    whenReady(db.execute(read(text("x"))))(_ shouldEqual None)

    // Get on a inserted key returns the inserted value.
    whenReady(db.execute(write(text("x"), real(1))))(_ shouldEqual real(1))
    whenReady(db.execute(read(text("x"))))(_ shouldEqual real(1))

    // Get on a modified key within a transaction returns the modified value.
    whenReady(db.execute(cons(write(text("x"), real(2)), read(text("x")))))(_ shouldEqual real(2))
  }

  test("Execute detects conflicts.") { db =>
    // Spy the underlying database to insert latches inside its put method that will enable
    // deterministic injection of race conditions into transaction execution.
    val ready = new CountDownLatch(1)
    val block = new CountDownLatch(1)
    val fake = spy[Database](db)

    doAnswer(new CallsRealMethods {
      override def answer(invocation: InvocationOnMock): Object = {
        println("Hi")
        ready.countDown()
        assert(block.await(10, TimeUnit.SECONDS))
        super.answer(invocation)
      }
    }).when(fake).cput(Map("x" -> 0L), Map.empty)(global)

    // Construct a read-only transaction and wait for it to reach the faked 'put' method. Then
    // construct a second transaction that updates the value of the field and unblocks the first
    // transaction after it completes execution. This introduces a conflict that should cause the
    // first transaction to fail.
    val exec = fake.execute(read(text("x")))
    assert(ready.await(10, TimeUnit.SECONDS))
    fake.execute(write(text("x"), text("foo"))).onComplete(_ => block.countDown())
    whenReady(exec.failed)(_ shouldBe an [Exception])
    whenReady(db.get(Set("x")))(_ should contain theSameElementsAs Seq("x" -> Revision(1L, text("foo"))))
  }

  test("Execute maintains mutable state.") { db =>
    // Verifies that local variables and loops are properly handled by the database execution logic.
    whenReady(db.execute(cons(
      store(text("i"), real(0)),
      cons(
        repeat(
          less(load(text("i")), real(3)),
          store(text("i"), add(load(text("i")), real(1)))
        ),
        load(text("i"))
      )
    )))(_ shouldEqual real(3))
  }

}
