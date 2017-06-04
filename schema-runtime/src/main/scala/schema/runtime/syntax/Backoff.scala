package schema.runtime
package syntax

import java.util.concurrent.ThreadLocalRandom
import scala.concurrent.duration._

object Backoff {

  /**
   * Returns a finite stream of exponential jittered backoffs. Implementation is based on the "Full
   * Jitter" algorithm described at http://www.awsarchitectureblog.com/2015/03/backoff.html.
   *
   * @param times Number of times to retry.
   * @param initial Initial duration.
   * @param maximum Maximum backoff.
   * @return Jittered exponential backoff.
   */
  def exponentialJittered(
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