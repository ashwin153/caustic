package com.schema.runtime.syntax

import com.schema.runtime._
import scala.language.dynamics

/**
 * A dynamically-typed, object. Proxies may represent a reference, a field, or an array. However,
 * its meaning is entirely dependent on the way that it is used. Implementation relies on the
 * scala.language.dynamics language feature to enable dynamic-typing.
 *
 * @param path Underlying object key.
 */
case class Proxy(path: Transaction) extends Dynamic {

  /**
   * Returns a proxy to the field or reference specified by name.
   *
   * @param name Field name.
   * @return Proxy to named field.
   */
  def selectDynamic(name: String): Proxy =
    Proxy(read(this.path ++ FieldDelimiter ++ name))

  /**
   * Returns a proxy to the specified array index.
   *
   * @param index Array index.
   * @return Proxy to array index.
   */
  def apply(index: Any): Proxy =
    Proxy(this.path ++ FieldDelimiter ++ index.toString)

  /**
   * Updates the field or reference with the specified name to the specified value. Implementation
   * also verifies that the field name exists in the list of fields for the corresponding object so
   * that the field may be safely removed when the corresponding object is deleted.
   *
   * @param name Field name.
   * @param value Updated value.
   */
  def updateDynamic(name: String)(value: Transaction)(
    implicit builder: Builder
  ): Unit = {
    val field = this.path ++ FieldDelimiter ++ name

    If (read(this.path).contains(field)) {
      Return(write(field, value))
    } Else {
      Return(cons(write(this.path, read(this.path) ++ field ++ ListDelimiter), write(field, value)))
    }
  }

  /**
   * Updates the specified array index to the specified value. Implementation also verifies that the
   * field name exists in the list of fields for the corresponding object so that the field may be
   * safely removed when the corresponding object is deleted.
   *
   * @param index Array index.
   * @param value Updated value.
   */
  def update(index: Any, value: Transaction)(
    implicit builder: Builder
  ): Unit = {
    val field = this.path ++ FieldDelimiter ++ index.toString

    If (read(this.path).contains(field)) {
      Return(write(field, value))
    } Else {
      Return(cons(write(path, read(path) ++ field ++ ListDelimiter), write(field, value)))
    }
  }

}