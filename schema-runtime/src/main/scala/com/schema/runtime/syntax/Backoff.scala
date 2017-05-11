package com.schema.runtime.syntax

import java.util.concurrent.ThreadLocalRandom
import scala.concurrent.duration.Duration

object Backoff {

  /**
   *
   * @param initial
   * @param maximum
   * @return
   */
  def exponential(
    initial: Duration,
    maximum: Duration
  ): Stream[Duration] = {
    require(initial > Duration.Zero, "Initial duration must be positive.")
    require(maximum >= initial, "Maximum duration must be greater than the initial.")

    // "Full Jitter" via http://www.awsarchitectureblog.com/2015/03/backoff.html.
    def next(attempt: Int): Stream[Duration] = {
      val shift = math.min(attempt, 62)
      val maxBackoff = maximum.min(initial * (1L << shift))
      val random = Duration.fromNanos(ThreadLocalRandom.current().nextLong(maxBackoff.toNanos))
      random #:: next(attempt + 1)
    }

    initial #:: next(1)
  }

}