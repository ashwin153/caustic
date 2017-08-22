package caustic.common.concurrent

import java.util.concurrent.ThreadLocalRandom
import java.util.{Timer, TimerTask}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future, Promise}

object Backoff {

  // Retry scheduler.
  lazy val scheduler: Timer = new Timer(true)

  /**
   * Asynchronously executes the specified function and returns the result and automatically retries
   * failures with the provided backoffs.
   *
   * @param backoffs Backoff durations.
   * @param f Function to execute.
   * @param ec Implicit execution context.
   * @tparam T Type of result.
   * @return Result of function or an exception on failure.
   */
  def retry[T](backoffs: Seq[FiniteDuration])(f: => Future[T])(
    implicit ec: ExecutionContext
  ): Future[T] =
    f.recoverWith {
      case _ if backoffs.nonEmpty =>
        val result = Promise[T]()

        this.scheduler.schedule(new TimerTask {
          override def run(): Unit = retry(backoffs.drop(1))(f).onComplete(result.complete)
        }, backoffs.head.toMillis)

        result.future
    }

  /**
   * Returns a finite stream of exponential jittered backoffs. Implementation is based on the "Full
   * Jitter" algorithm described at http://www.awsarchitectureblog.com/2015/03/backoff.html.
   *
   * @param times Number of times to retry.
   * @param initial Initial duration.
   * @param maximum Maximum backoff.
   * @return Jittered exponential backoff.
   */
  def exponential(
    times: Int,
    initial: FiniteDuration,
    maximum: FiniteDuration
  ): Stream[FiniteDuration] = {
    require(initial > Duration.Zero, "Initial duration must be positive.")
    require(maximum >= initial, "Maximum duration must be greater than the initial.")
    require(times > 0, "Times must be a positive integer.")

    def next(attempt: Int): Stream[FiniteDuration] = {
      val shift = math.min(attempt, 62)
      val maxBackoff = maximum.min(initial * (1L << shift))
      val random = Duration.fromNanos(ThreadLocalRandom.current().nextLong(maxBackoff.toNanos))
      if (attempt >= times) Stream.empty else random #:: next(attempt + 1)
    }

    initial #:: next(1)
  }

}
