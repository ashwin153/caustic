package caustic.runtime.service

import java.util.concurrent.ThreadLocalRandom
import scala.concurrent.duration.{Duration, FiniteDuration}

object Backoff {

  /**
   * Returns a stream of constantly jittered backoffs.
   *
   * @param times Number of attempts.
   * @param interval Backoff duration.
   * @return Constantly jittered backoffs.
   */
  def constant(
    times: Int,
    interval: FiniteDuration
  ): Seq[FiniteDuration] = {
    require(times > 0, "Times must be positive.")
    require(interval > Duration.Zero, "Interval must be positive.")
    Seq.fill(times)(Duration.fromNanos(ThreadLocalRandom.current().nextLong(interval.toNanos)))
  }

  /**
   * Returns a stream of exponentially jittered backoffs. Based on the "Full Jitter" algorithm
   * described in http://www.awsarchitectureblog.com/2015/03/backoff.html and implemented in the
   * Finagle project. Recommended default backoff policy.
   *
   * @param times Number of attempts.
   * @param initial Initial backoff duration.
   * @param maximum Maximum backoff duration.
   * @return Exponentially jittered backoffs.
   */
  def exponential(
    times: Int,
    initial: FiniteDuration,
    maximum: FiniteDuration
  ): Stream[FiniteDuration] = {
    require(times > 0, "Times must be positive.")
    require(initial > Duration.Zero, "Initial duration must be positive.")
    require(maximum >= initial, "Maximum duration must be greater than the initial.")

    def next(attempt: Int): Stream[FiniteDuration] = {
      val shift = math.min(attempt, 62)
      val maxBackoff = maximum.min(initial * (1L << shift))
      val random = Duration.fromNanos(ThreadLocalRandom.current().nextLong(maxBackoff.toNanos))
      if (attempt >= times) Stream.empty else random #:: next(attempt + 1)
    }

    initial #:: next(1)
  }

}