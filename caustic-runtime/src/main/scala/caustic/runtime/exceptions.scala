package caustic.runtime

import caustic.common.concurrent.Backoff.NonRetryable

/**
 *
 * @param keys
 */
case class ConflictException(keys: Set[Key]) extends Exception

/**
 *
 * @param message
 */
case class ExecutionException(message: String) extends Exception with NonRetryable