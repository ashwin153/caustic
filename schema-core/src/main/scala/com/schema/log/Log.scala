package com.schema.log

import scala.concurrent.Future

/**
 * An asynchronous log. Logs are thread-safe.
 *
 * @tparam T Type of log entries.
 */
trait Log[T] {

  /**
   * Asynchronously appends some data to the log. Returns a future containing the log sequence
   * number (sn), which completes when the data is successfully appended. Each lsn is guaranteed to
   * (1) be non-negative, and (2) be larger for entries appended after.
   *
   * @param data Data to be appended.
   * @return Future containing the lsn number of the appended entry.
   */
  def append(data: T): Future[Record[T]]

  /**
   * Returns an [[Cursor]] beginning from the specified lsn. Traversal of this cursor asynchronously
   * produces all log records whose lsn is greater than or equal to the specified lsn in the order
   * that they appear in the log.
   *
   * @param from Initial lsn.
   * @return Asynchronous cursor over log entries.
   */
  def read(from: Long): Cursor[T]

}