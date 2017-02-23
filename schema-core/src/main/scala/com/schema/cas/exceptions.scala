package com.schema.cas

/**
 *
 * @param message
 * @param cause
 */
case class TransactionRejectedException(
  message: String = "",
  cause: Throwable = null
) extends Exception(message, cause)

/**
 *
 * @param message
 * @param cause
 */
case class RetriesExhaustedException(
  message: String = "",
  cause: Throwable = null
) extends Exception(message, cause)
