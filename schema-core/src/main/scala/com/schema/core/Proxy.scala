package com.schema.core

import shapeless.{HList, LabelledGeneric, Witness}
import shapeless.labelled.FieldType
import shapeless.ops.record.{Selector, ToMap, Updater}

/**
 * A statically-typed, dynamic proxy object. Proxies delegate field-level accesses and modifications
 * to some underlying implementation in a type-safe way. Because proxies rely on Shapeless to
 * guarantee static type safety, they may only be used for types for which a [[LabelledGeneric]]
 * representation can be implicitly materialized (ie. case classes).
 *
 * @tparam T Type of proxied object.
 */
trait Proxy[T] {

  /**
   * Returns a value with the static type of the field whose name matches the specified witness. For
   * example, suppose we've defined some case class Foo(bar: String) and we have some variable x of
   * type Proxy[Foo]. Then, x.get('bar) will return a value of type String and x.get('hello) will
   * throw a compile-time error because no field named hello exists for the type Foo.
   *
   * @param witness Field name witness specified by symbol.
   * @param gen Implicit generic representation.
   * @param selector Implicit field selector.
   * @tparam R Type of generic representation.
   * @tparam K Type of field name symbol.
   * @tparam V Type of field value.
   * @return Value with the static type of the field matching the specified witness.
   */
  def get[R <: HList, K <: Symbol, V](witness: Witness.Lt[K])(
    implicit gen: LabelledGeneric.Aux[T, R],
    selector: Selector.Aux[R, K, V]
  ): V

  def apply[R <: HList, K <: Symbol, V](witness: Witness.Lt[K])(
    implicit gen: LabelledGeneric.Aux[T, R],
    selector: Selector.Aux[R, K, V]
  ): V = get(witness)

  /**
   * Updates the field whose name matches the specified witness to the specified value For example,
   * suppose we've defined some case class Foo(bar: String) and we have some variable x of type
   * Proxy[Foo]. Then, x.set('bar, "Hello") updates the value of bar to "Hello". However,
   * x.set('bar, 123) will throw a compile-time error because bar is of type String and
   * x.set('hello, "Hello") will throw a compile-time error because no field named hello exists for
   * the type Foo.
   *
   * @param witness Field name witness specified by symbol.
   * @param value Field value to update.
   * @param gen Implicit generic representation.
   * @param updater Implicit field updater.
   * @tparam R Type of generic representation.
   * @tparam K Type of field name symbol.
   * @tparam V Type of field value.
   */
  def set[R <: HList, K <: Symbol, V](witness: Witness.Lt[K], value: V)(
    implicit gen: LabelledGeneric.Aux[T, R],
    updater: Updater.Aux[R, FieldType[K, V], R]
  ): Unit

  def update[R <: HList, K <: Symbol, V](witness: Witness.Lt[K], value: V)(
    implicit gen: LabelledGeneric.Aux[T, R],
    updater: Updater.Aux[R, FieldType[K, V], R]
  ): Unit = set(witness, value)

}