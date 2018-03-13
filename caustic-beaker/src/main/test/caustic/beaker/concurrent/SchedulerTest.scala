package caustic.beaker.concurrent

import caustic.beaker.common.{Relation, Scheduler}
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}
import java.util.concurrent.{CountDownLatch, TimeUnit}
import scala.util.Try

@RunWith(classOf[JUnitRunner])
class SchedulerTest extends FunSuite with Matchers with ScalaFutures {

  test("Related tasks are sequential") {
    val scheduler = new Scheduler[Int](Relation.Total)
    val scheduled = new CountDownLatch(1)
    val latch     = new CountDownLatch(1)

    // Schedules all tasks sequentially.
    scheduler.submit(0)(_ => Try(scheduled.await()))
    val x = scheduler.submit(1)(_ => Try(latch.await(50, TimeUnit.MILLISECONDS)))
    val y = scheduler.submit(2)(_ => Try(latch.countDown()))
    scheduled.countDown()

    // X will fail if and only if it is strictly executed before Y.
    whenReady(x)(r => r shouldBe false)
    scheduler.close()
  }

  test("Unrelated tasks are concurrent") {
    val scheduler = new Scheduler[Int](Relation.Identity)
    val scheduled = new CountDownLatch(1)
    val latch     = new CountDownLatch(2)

    // Schedules all tasks concurrently.
    scheduler.submit(0)(_ => Try(scheduled.await()))
    val x = scheduler.submit(1) { _ => latch.countDown(); Try(latch.await(50, TimeUnit.MILLISECONDS)) }
    val y = scheduler.submit(2) { _ => latch.countDown(); Try(latch.await(50, TimeUnit.MILLISECONDS)) }
    scheduled.countDown()

    // X and Y will both succeed if and only if they are executed concurrently.
    whenReady(x)(r => r shouldEqual true)
    whenReady(y)(r => r shouldEqual true)
    scheduler.close()
  }

}
