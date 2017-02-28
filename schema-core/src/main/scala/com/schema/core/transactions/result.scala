package com.schema.core.transactions

/**
 * A desired result of applying a Transaction. Transactions that result in [[Rollback]] do not
 * wish to be attempted and transactions that result in [[Commit]] do.
 *
 * @tparam T Type of result.
 */
sealed trait Result[+T]
case class Commit[+T](result: T) extends Result[T]
case class Rollback[+T](result: T) extends Result[T]