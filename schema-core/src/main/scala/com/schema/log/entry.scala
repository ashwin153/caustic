package com.schema.log

/**
 * A log entry. Log entries are associated with a monotonically increasing log sequence number (lsn)
 * that indicates the relative position of the entry in the log. Each lsn is guaranteed to (1) be
 * non-negative, and (2) be larger for entries appended after. Log entries may either be of the type
 * [[Record]], in which case they are associated with some payload in the log, or of the type
 * [[Pending]], which indicates that no more records currently remain in the log.
 */
sealed trait Entry[T] { val lsn: Long }
case class Record[T](lsn: Long, payload: T) extends Entry[T]
case class Pending[T](lsn: Long) extends Entry[T]