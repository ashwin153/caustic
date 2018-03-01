package caustic.runtime

import caustic.runtime.Retry.NonRetryable

import org.junit.runner.RunWith
import org.scalatest.{AsyncFunSuite, Matchers}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class RetryTest extends AsyncFunSuite with MockitoSugar with Matchers{

  test("Retries all retryable exceptions") {
    val results = Seq(Failure(new Exception), Success(())).iterator
    Retry.attempt(Seq.fill(1)(Duration.Zero))(results.next()).map(_ shouldBe ())
  }

  test("Retries until backoffs are exhausted") {
    val results = Seq(Failure(new Exception), Failure(new Exception), Success(())).iterator
    recoverToSucceededIf[Exception](Retry.attempt(Seq.fill(1)(Duration.Zero))(results.next()))
  }

  test("Retries prevented for non-retryable exceptions") {
    val results = Seq(Failure(new Exception with NonRetryable), Success(())).iterator
    recoverToSucceededIf[Exception](Retry.attempt(Seq.fill(1)(Duration.Zero))(results.next()))
  }

}
