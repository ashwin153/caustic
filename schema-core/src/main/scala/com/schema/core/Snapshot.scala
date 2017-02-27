package com.schema.core

import com.schema.core.transactions.Instruction
import scala.concurrent.Future

/**
 * A mutable key-value store. Snapshots map unique string identifiers to arbitrary values. Snapshots
 * may store values of any type; therefore, values must be dynamically cast at runtime in order to
 * be used for any useful purpose. Despite these limitations, it is possible to recover static type
 * safety using a [[Schema]].
 */
trait Snapshot {

  def apply(key: String): Any = get(key).get

  def get(key: String): Option[Any]

  def contains(key: String): Boolean = get(key).isDefined

  def +=(kv: (String, Any)): Unit

  def -=(key: String): Unit

}
