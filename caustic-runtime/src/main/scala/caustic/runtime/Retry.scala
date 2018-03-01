package caustic.runtime

import java.util.{Timer, TimerTask}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}
import scala.util.Try
import scala.util.control.NonFatal

object Retry {

  // Retry scheduler.
  lazy val scheduler: Timer = new Timer(true)

  /**
   * A marker trait for non-retryable failures. All NonFatal exceptions are considered to be
   * retryable by default. Automatic retries should only be avoiding on extraordinary failures.
   */
  trait NonRetryable

  /**
   * Returns the result of the asynchronous task, and retries failures with backoff. Guarantees that
   * retries will be scheduled at some time after each backoff interval.
   *
   * @param backoffs Backoff durations.
   * @param f Fallible task.
   * @return Result of task or exception on successive failure.
   */
  def attempt[T](backoffs: Seq[FiniteDuration])(f: => Try[T]): Future[T] = {
    Future.fromTry(f).recoverWith {
      case NonFatal(e) if !e.isInstanceOf[NonRetryable] && backoffs.nonEmpty =>
        // Schedule the retries on the underlying timer.
        val result = Promise[T]()
        this.scheduler.schedule(new TimerTask {
          override def run(): Unit =
            attempt(backoffs.drop(1))(f).onComplete(result.complete)
        }, backoffs.head.toMillis)

        // Return a handle to the retry attempt.
        result.future
    }
  }

}