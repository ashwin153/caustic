package com.schema.runtime
package syntax

import scala.language.dynamics

/**
 * A dynamically-typed, object. Proxies may represent a reference, a field, or an array. However,
 * its meaning is entirely dependent on the way that it is used. Implementation relies on the
 * scala.language.dynamics language feature to enable dynamic-typing.
 *
 * @param key Underlying object key.
 * @param owner Owning object.
 */
case class Proxy(key: Transaction, owner: Option[Proxy]) extends Dynamic {

  /**
   *
   * @param name
   * @return
   */
  def selectDynamic(name: String): Proxy = owner match {
    case None =>
      // Interpret key as an object and name as a field.
      Proxy(this.key ++ FieldDelimiter ++ name, Some(this))
    case Some(o) =>
      // Interpret key as a reference and name as field on the referenced object.
      Proxy(read(this.key) ++ FieldDelimiter ++ name, Some(Proxy(read(this.key), None)))
  }

  /**
   *
   * @param index
   * @return
   */
  def apply(index: Any): Proxy = {
    // Interpret key as an array.
    Proxy(this.key ++ FieldDelimiter ++ index.toString, Some(this))
  }

  /**
   *
   * @param name
   * @param index
   * @return
   */
  def applyDynamics(name: String)(index: Any): Proxy = {
    // Interpret key as an object and name as an array field.
    Proxy(this.key ++ FieldDelimiter ++ name ++ FieldDelimiter ++ index.toString, Some(this))
  }

  /**
   *
   * @param index
   * @param value
   * @param ctx
   */
  def update(index: Any, value: Transaction)(implicit ctx: Context): Unit =
    updateDynamic(index.toString)(value)

  /**
   *
   * @param name
   * @param value
   * @param ctx
   */
  def updateDynamic(name: String)(value: Transaction)(implicit ctx: Context): Unit = {
    val field = this.path ++ FieldDelimiter ++ name

    // Verify that the field is contained in the objects field collection.
    this.owner match {
      case None => Index(this.fields) += field
      case Some(o) => Index(o.fields) += field
    }
  }

}
