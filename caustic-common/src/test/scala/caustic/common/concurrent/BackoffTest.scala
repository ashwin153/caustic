package caustic.common.concurrent

import Backoff._

import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, TimeoutException}
import scala.language.postfixOps

@RunWith(classOf[JUnitRunner])
class BackoffTest extends FunSuite
  with ScalaFutures
  with Matchers
  with MockitoSugar {

  test("Backoff properly retries failures.") {
    val results = Iterator(
      Future.failed[String](new Exception("Retryable.")),
      Future("Success"),
      Future.failed[String](new Exception("Retryable.")),
      Future("Success")
    )

    // Verify that retries succeed on retryable failures.
    whenReady(retry(Seq(100 millis))(results.next()))(_ shouldEqual "Success")

    // Verify that retry respects backoff durations.
    assertThrows[TimeoutException] {
      Await.result(retry(Seq(1 second))(results.next()), 500 millis)
    }
  }

}
