package com.schema.runtime.syntax

import com.schema.runtime.{literal, _}
import scala.language.dynamics

/**
 *
 */
sealed trait Proxy extends Dynamic {

  /**
   *
   * @return
   */
  def key: Transaction

  /**
   *
   * @param name
   * @return
   */
  def selectDynamic(name: String): Field

  /**
   *
   * @param name
   * @param index
   * @return
   */
  def applyDynamic(name: String)(index: Transaction): Field

  /**
   *
   * @param name
   * @param value
   * @param ctx
   */
  def updateDynamic(name: String)(value: Transaction)(implicit ctx: Context): Unit

}

/**
 *
 */
case class Object(key: Transaction) extends Proxy {

  override def selectDynamic(field: String): Field =
    Field(this.key ++ FieldDelimiter ++ literal(field), field, this)

  override def applyDynamic(field: String)(index: Transaction): Field =
    Field(this.key ++ FieldDelimiter ++ literal(field) ++ FieldDelimiter ++ index, field, this)

  override def updateDynamic(field: String)(value: Transaction)(implicit ctx: Context): Unit = {
    // Verify that the owning object exists.
    If (!equal(this, Literal.True)) {
      ctx += write(this.key, Literal.True)
    }

    // Verify that the field name is recorded on the object.
    val names = this.key ++ FieldDelimiter ++ "$fields"
    If (!read(names).contains(field)) {
      ctx += write(names, read(names) ++ literal(field) ++ ListDelimiter)
    }

    // Append the field update to the context.
    val path = this.key ++ FieldDelimiter ++ literal(field)
    ctx += write(path, value)
  }

}

/**
 *
 * @param key
 * @param owner
 */
case class Field(key: Transaction, name: String, owner: Object) extends Proxy {

  override def selectDynamic(field: String): Field =
    Field(read(this.key) ++ FieldDelimiter ++ literal(field), field, Object(read(this.key)))

  override def applyDynamic(field: String)(index: Transaction): Field =
    Field(read(this.key) ++ FieldDelimiter ++ literal(field) ++ FieldDelimiter ++ index, field, Object(read(this.key)))

  override def updateDynamic(field: String)(value: Transaction)(implicit ctx: Context): Unit = {
    // Verify that the owning object exists.
    If (!equal(this.owner, Literal.True)) {
      ctx += write(this.owner.key, Literal.True)
    }

    // Verify that the field name is recorded on the owner object.
    val names = this.owner.key ++ FieldDelimiter ++ "$fields"
    If (!read(names).contains(field)) {
      ctx += write(names, read(names) ++ literal(field) ++ ListDelimiter)
    }

    // Append the field update to the context.
    val path = read(this.key) ++ FieldDelimiter ++ literal(field)
    ctx += write(path, value)
  }

  /**
   *
   * @param at
   * @param value
   * @param ctx
   */
  def update(at: Transaction, value: Transaction)(implicit ctx: Context): Unit = {
    // Append the array update to the context.
    val path = this.key ++ FieldDelimiter ++ at
    ctx += write(path, value)

    // Verify that the owning object exists.
    If (!equal(this.owner, Literal.True)) {
      ctx += write(this.owner.key, Literal.True)
    }

    // Verify that the index name is recorded on the owner object.
    val names = this.owner.key ++ FieldDelimiter ++ "$indices"
    If (!read(names).contains(this.name)) {
      ctx += write(names, read(names) ++ literal(this.name) ++ ListDelimiter)
    }

    // Verify that the at is recorded on the index.
    val index = this.key ++ FieldDelimiter ++ literal("$values")
    If (!read(index).contains(at)) {
      ctx += write(index, read(index) ++ at ++ ListDelimiter)
    }
  }

}
