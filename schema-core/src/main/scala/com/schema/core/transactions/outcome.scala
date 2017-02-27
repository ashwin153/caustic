package com.schema.core

/**
 * An outcome of applying a transaction. Any transaction which resulted in [[Rollback]] will not
 * be attempted and will produce an outcome of [[Rollbacked]]. Any transaction which resulted in
 * [[Commit]] will be attempted and will produce an outcome of [[Committed]] if it was successfully
 * applied and [[Failed]] otherwise.
 *
 * @tparam T Type of outcome result.
 */
sealed trait Outcome[+T]
case class Committed[+T](result: T) extends Outcome[T]
case class Rollbacked[+T](result: T) extends Outcome[T]
case class Failed[+T](throwable: Throwable) extends Outcome[T]
