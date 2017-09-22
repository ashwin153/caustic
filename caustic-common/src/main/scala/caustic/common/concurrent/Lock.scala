package caustic.common.concurrent

import java.util.concurrent.locks.StampedLock

/**
 * A Scala wrapper around the Java8 [[StampedLock]]. Provides a simple abstraction that hides the
 * semantics of acquiring and releasing shared, exclusive, and optimistic locks. The lock is not
 * reentrant. The [[StampedLock]] implementation benchmarks significantly better than standard
 * monitor synchronization and the various lock abstractions in the Java concurrency package.
 *
 * @param underlying Underlying lock.
 */
class Lock(underlying: StampedLock) {

  /**
   * Returns the result of the provided code block, after acquiring a shared read lock. Because
   * shared locks can be acquired concurrently, the provided function must be side-effect free.
   *
   * @param block Critical section; side-effect free.
   * @return Result of evaluation.
   */
  def shared[T](block: => T): T = {
    val stamp = this.underlying.readLock()
    try {
      block
    } finally {
      this.underlying.unlockRead(stamp)
    }
  }

  /**
   * Returns the result of the provided code block, after acquiring an exclusive write lock. Because
   * exclusive locks may not be acquired concurrently, the provided function may have side-effects.
   *
   * @param block Critical section.
   * @return Result of evaluation.
   */
  def exclusive[T](block: => T): T = {
    val stamp = this.underlying.writeLock()
    try {
      block
    } finally {
      this.underlying.unlockWrite(stamp)
    }
  }

  /**
   * Returns the result of the provided code block, without acquiring a lock. If an exclusive lock
   * was acquired during execution, then the code block is reevaluated under a shared lock. Because
   * the block may be executed multiple times, the provided function must be side-effect free.
   *
   * @param block Critical section; side-effect free.
   * @return Result of evaluation.
   */
  def optimistic[T](block: => T): T = {
    val stamp = this.underlying.tryOptimisticRead()
    val value = block
    if (!this.underlying.validate(stamp)) shared(block) else value
  }

}

object Lock {

  /**
   * Constructs a lock backed by a Java StampedLock.
   *
   * @return Lock.
   */
  def apply(): Lock = new Lock(new StampedLock)

}

