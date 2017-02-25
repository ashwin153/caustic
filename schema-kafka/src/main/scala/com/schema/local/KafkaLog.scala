package com.schema.local

import com.schema.log.{Cursor, Log, Record}
import scala.concurrent.Future

/**
 *
 * @tparam T Type of log entries.
 */
class KafkaLog[T] extends Log[T] {

  override def append(data: T): Future[Record[T]] = ???

  override def read(from: Long): Cursor[T] = ???

}
