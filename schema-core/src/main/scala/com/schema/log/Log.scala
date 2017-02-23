package com.schema.log

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

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

object Log {

  /**
   * Constructs an empty log from an empty [[LocalLog]] backed by an [[mutable.ArrayBuffer]].
   * Therefore, append and random access on the default log can be performed in O(1).
   *
   * @tparam T Type of log entries.
   * @return Empty log.
   */
  def empty[T]: LocalLog[T] = new LocalLog[T](mutable.ArrayBuffer.empty)

  /**
   * An in-memory log. Local logs append records to a [[mutable.Buffer]] and assign a log sequence
   * number to each record based on their index in this buffer. Local log cursors are implemented by
   * iterating over the underlying buffer.
   *
   * @param records Initial log records.
   * @tparam T Type of log entries.
   */
  class LocalLog[T](records: mutable.Buffer[T]) extends Log[T] {

    override def append(data: T): Future[Record[T]] = {
      // Buffers can only index 4 bytes, therefore we must guard against overflow.
      require(this.records.size < Int.MaxValue, "Log records overflowed.")

      // Append the data to the log in a thread-safe way.
      this.records.synchronized {
        this.records += data
        Future(Record(this.records.size.toLong - 1, data))
      }
    }

    override def read(from: Long): Cursor[T] = new Cursor[T] {
      var position: Int = from.toInt

      override def next(): Future[Entry[T]] = {
        // Buffers can only index 4 bytes, therefore we must guard against overflow.
        require(this.position >= 0, "Cursor position must be positive.")

        // Retrieve the element from the buffer at the current position.
        if (this.position >= records.size) {
          Future(Pending(this.position))
        } else {
          val res = Future(Record(this.position, records(this.position)))
          this.position += 1
          res
        }
      }
    }

  }

}