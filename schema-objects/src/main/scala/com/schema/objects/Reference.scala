package com.schema.objects

import shapeless.LabelledGeneric

/**
 * An object reference. Because a [[Schema]] stores objects independently from their fields, an
 * object that is referenced by another will be duplicated in the snapshot - once as an object
 * and once as a field of the referencing object. References mitigate the problem by storing only
 * the object identifier in referencing objects.
 *
 * @param refersTo Object identifier of reference.
 * @param gen Implicit generic representation.
 * @tparam T Type of object reference.
 */
case class Reference[T](refersTo: String)(implicit gen: LabelledGeneric[T]) {

  /**
   * Dereferences the reference using the implied schema and returns a proxy object to its
   * contents. Assumes the reference exists; if not, then this method will throw a
   * [[NoSuchElementException]]. It is the responsibility of the user to clean up dangling
   * references.
   *
   * @param schema Implicit schema.
   * @return Proxy to the object specified by the reference.
   * @throws NoSuchElementException If the reference object does not exist.
   */
  def ->(implicit schema: Schema): Proxy[T] =
    schema.select[T](this.refersTo).get

}