package com.schema.runtime

import scala.language.dynamics

/**
 * A dynamically-typed, object. Proxies may represent a reference, a field, or an array. However,
 * its meaning is entirely dependent on the way that it is used. Implementation relies on the
 * scala.language.dynamics language feature to enable dynamic-typing.
 *
 * @param key Underlying object key.
 */
case class Proxy(key: Transaction, owner: Option[Transaction]) extends Dynamic {

  def selectDynamic(name: String): Proxy = owner match {
    case None =>
      // Interpret key as an object and name as a field.
      Proxy(this.key ++ FieldDelimiter ++ name, Some(this.key))
    case Some(o) =>
      // Interpret key as a reference and name as field on the referenced object.
      Proxy(read(this.key) ++ FieldDelimiter ++ name, Some(read(this.key)))
  }

  def apply(index: Any): Proxy = {
    // Interpret key as an array.
    Proxy(this.key ++ FieldDelimiter ++ index.toString, Some(this.key))
  }

  def applyDynamics(name: String)(index: Any): Proxy = {
    // Interpret key as an object and name as an array field.
    Proxy(this.key ++ FieldDelimiter ++ name ++ FieldDelimiter ++ index.toString, Some(this.key))
  }

  def update(index: Any, value: Transaction)(implicit ctx: Context): Unit =
    updateDynamic(index.toString)(value)
  
  def updateDynamic(name: String)(value: Transaction)(implicit ctx: Context): Unit = {
    val field = this.path ++ FieldDelimiter ++ name

    // Check that the owning object contains the field name before modifying the field. This is
    // necessary to ensure consistent, safe deletes to the system.
    owner match {
      case None =>
        ctx :+ branch(
          read(this.key) contains field,
          write(field, value),
          cons(write(this.key, read(this.key) ++ field ++ ListDelimiter), write(field, value))
        )
      case Some(o) =>
        ctx :+ branch(
          read(o) contains field,
          write(field, value),
          cons(write(this.key, read(this.key) ++ field ++ ListDelimiter), write(field, value))
        )
    }
  }

}
