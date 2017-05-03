package com.schema.runtime.syntax

import com.schema.runtime._
import scala.language.dynamics

/**
 *
 * @param key
 */
case class Object(key: Transaction) extends Dynamic {

  /**
   * Fields + References.
   *
   * @param name
   * @return
   */
  def selectDynamic(name: String): Object =
    Object(read(key ++ FieldDelimiter ++ name))

  /**
   * Arrays.
   *
   * @param name
   * @param index
   * @return
   */
  def applyDynamic(name: String)(index: Any): Object =
    Object(read(key ++ FieldDelimiter ++ name ++ FieldDelimiter ++ index.toString))

  /**
   * Field updates + References.
   *
   * @param name
   * @param value
   */
  def updateDynamic(name: String)(value: Transaction)(implicit builder: Builder): Unit = {
    val field = key ++ FieldDelimiter ++ name
    If (read(key).contains(field)) {
      Return(write(field, value))
    } Else {
      Return(cons(write(key, read(key) ++ field ++ ListDelimiter), write(field, value)))
    }
  }

  /**
   * Array updates.
   *
   * @param index
   * @param value
   * @return
   */
  def update(index: Any, value: Transaction)(implicit builder: Builder) = {
    val field = key ++ FieldDelimiter ++ index.toString
    If (read(key).contains(field)) {
      Return(write(field, value))
    } Else {
      Return(cons(write(key, read(key) ++ field ++ ListDelimiter), write(field, value)))
    }
  }

}