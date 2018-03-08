package caustic.beaker.concurrent

import caustic.beaker.ordering.Relation
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}

import java.util.concurrent.{CountDownLatch, TimeUnit}
import scala.util.Try

@RunWith(classOf[JUnitRunner])
class ExecutorTest extends FunSuite with Matchers with ScalaFutures {

  test("Conflicting tasks are sequential.") {
    // Schedule all tasks sequentially.
    val executor = new Executor[Int](Relation.Total)
    val barrier = new CountDownLatch(1)
    val x = executor.submit(0)(_ => Try(barrier.await(50, TimeUnit.MILLISECONDS)))
    val y = executor.submit(1)(_ => Try(barrier.countDown()))

    // X will fail if and only if it is strictly executed before Y.
    whenReady(x)(r => r shouldBe false)
    executor.close()
  }

  test("Disjoint tasks are concurrent.") {
    // Schedule all tasks concurrently.
    val executor = new Executor[Int](Relation.Identity)
    val barrier = new CountDownLatch(2)
    val x = executor.submit(0)(_ => Try { barrier.countDown(); barrier.await(50, TimeUnit.MILLISECONDS) })
    val y = executor.submit(1)(_ => Try { barrier.countDown(); barrier.await(50, TimeUnit.MILLISECONDS) })

    // X and Y will both succeed if and only if they are executed concurrently.
    whenReady(x)(r => r shouldEqual true)
    whenReady(y)(r => r shouldEqual true)
    executor.close()
  }

}
