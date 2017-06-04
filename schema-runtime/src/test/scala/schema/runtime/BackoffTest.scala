package schema.runtime

import Backoff._
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}
import org.scalatest.mockito.MockitoSugar
import scala.concurrent.{Await, Future, TimeoutException}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class BackoffTest extends FunSuite
  with ScalaFutures
  with Matchers
  with MockitoSugar {

  test("Backoff properly retries failures.") {
    val fake = mock[Database]
    when(fake.execute(literal(""))(global))
      .thenReturn(Future.failed(new Exception("Retryable.")))
      .thenReturn(Future("Success"))
      .thenReturn(Future.failed(FatalException("Non-Retryable")))
      .thenReturn(Future.failed(new Exception("Retryable")))
      .thenReturn(Future("Success"))

    // Verify that retries succeed on retryable failures.
    whenReady(retry(Seq(100 millis))(fake.execute(literal(""))))(_ shouldEqual "Success")

    // Verify that retries fail on fatal errors.
    whenReady(retry(Seq(100 millis))(fake.execute(literal(""))).failed)(_ shouldBe an [FatalException])

    // Verify that retry respects backoff durations.
    assertThrows[TimeoutException] {
      Await.result(retry(Seq(1 second))(fake.execute(literal(""))).failed, 500 millis)
    }

    verify(fake, times(4)).execute(literal(""))(global)
  }

}
