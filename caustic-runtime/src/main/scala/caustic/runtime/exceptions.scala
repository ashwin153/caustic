package caustic.runtime

import caustic.runtime.Retry.NonRetryable

/**
 * A retryable failure that is thrown when a subset of keys cause a conflict.
 *
 * @param keys Conflicting keys.
 */
case class ConflictException(keys: Set[Key]) extends Exception

/**
 * A non-retryable failure that is thrown when a transaction is malformed.
 *
 * @param message Error message.
 */
case class ExecutionException(message: String) extends Exception with NonRetryable