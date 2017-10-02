package caustic.common.concurrent

import java.util.concurrent.ThreadLocalRandom
import java.util.{Timer, TimerTask}
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

object Backoff {

  // Retry scheduler.
  lazy val scheduler: Timer = new Timer(true)

  /**
   * A marker trait for non-retryable failures.
   */
  trait NonRetryable

  /**
   * Returns the result of the asynchronous task, and retries all non-fatal, retryable failures.
   *
   * @param backoffs Backoff durations.
   * @param f Fallible task.
   * @param ec Implicit execution context.
   * @return Result of task or exception on retried failure.
   */
  def retry[T](backoffs: Seq[FiniteDuration])(f: => Future[T])(
    implicit ec: ExecutionContext
  ): Future[T] =
    f.recoverWith {
      case NonFatal(e) if !e.isInstanceOf[NonRetryable] && backoffs.nonEmpty =>
        // Schedule the retries on the underlying timer.
        val result = Promise[T]()
        this.scheduler.schedule(new TimerTask {
          override def run(): Unit =
            retry(backoffs.drop(1))(f).onComplete(result.complete)
        }, backoffs.head.toMillis)

        // Return a handle to the retry attempt.
        result.future
    }

  /**
   * Returns a finite stream of exponentially-jittered backoffs. Implementation is based on the
   * "Full Jitter" algorithm described in  http://www.awsarchitectureblog.com/2015/03/backoff.html
   * and implemented in the Finagle project. Recommended default backoff policy.
   *
   * @param times Number of retry attempts.
   * @param initial Initial backoff duration.
   * @param maximum Maximum backoff duration.
   * @return Exponentially-jittered backoffs.
   */
  def exponential(
    times: Int,
    initial: FiniteDuration,
    maximum: FiniteDuration
  ): Stream[FiniteDuration] = {
    require(initial > Duration.Zero, "Initial duration must be positive.")
    require(maximum >= initial, "Maximum duration must be greater than the initial.")
    require(times > 0, "Times must be positive.")

    def next(attempt: Int): Stream[FiniteDuration] = {
      val shift = math.min(attempt, 62)
      val maxBackoff = maximum.min(initial * (1L << shift))
      val random = Duration.fromNanos(ThreadLocalRandom.current().nextLong(maxBackoff.toNanos))
      if (attempt >= times) Stream.empty else random #:: next(attempt + 1)
    }

    initial #:: next(1)
  }

}
