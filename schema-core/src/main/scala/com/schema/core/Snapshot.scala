package com.schema.core

/**
 * A mutable key-value cache. Snapshots are in-memory or on-disk caches that map unique string
 * identifiers to arbitrarily typed objects.
 */
trait Snapshot {

  def apply(key: String): Any = get(key).get

  def get(key: String): Option[Any]

  def contains(key: String): Boolean = get(key).isDefined

  def +=(kv: (String, Any)): Unit

  def -=(key: String): Unit

}