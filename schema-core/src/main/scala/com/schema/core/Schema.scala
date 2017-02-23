package com.schema.core

import shapeless._
import shapeless.labelled._
import shapeless.ops.record._
import com.schema.core.Schema._

/**
 * An object interface over a snapshot. Schemas store objects separately from their fields to
 * provide independent access and modifications. Schemas utilize [[Proxy]] objects to guarantee that
 * the (1) the snapshot contains the only copy of each object and its fields and (2) every object
 * access and modification is performed directly on the snapshot.
 *
 * @param snapshot Underlying snapshot.
 */
class Schema(snapshot: Snapshot) {

  /**
   * Selects the object with the specified identifier and type. Returns 'None' if the object does
   * not exist in the snapshot, or returns a [[Proxy]] object backed this schema. In other words,
   * field-level accesses and modifications to the proxy object are fulfilled by the schema itself.
   * Note: operations on a proxy of a deleted object have undefined behavior.
   *
   * @param id Object identifier.
   * @param gen Implicit labled generic for type safety.
   * @tparam T Type of object.
   * @return Proxy object, or None if the identifier doesn't exist.
   */
  def select[T](id: String)(
    implicit gen: LabelledGeneric[T]
  ): Option[Proxy[T]] = {
    require(!id.contains(FieldDelimiter), "Identifier may not contain field delimiter.")

    this.snapshot.get(id).map(_ => new Proxy[T] {
      override def get[R <: HList, K <: Symbol, V](witness: Witness.Lt[K])(
        implicit gen: LabelledGeneric.Aux[T, R],
        selector: Selector.Aux[R, K, V]
      ): V =
        snapshot.get(id + FieldDelimiter + witness.value.name).get.asInstanceOf[V]

      override def set[R <: HList, K <: Symbol, V](witness: Witness.Lt[K], value: V)(
        implicit gen: LabelledGeneric.Aux[T, R],
        updater: Updater.Aux[R, FieldType[K, V], R]
      ): Unit =
        snapshot += (id + FieldDelimiter + witness.value.name) -> value
    })
  }

  /**
   * Inserts the object with the specified identifier to the snapshot. Identifiers must be globally
   * unique and may not contain the reserved field delimiter sequence. Implementation inserts the
   * object and its various fields as separate entries in the snapshot to ensure that they may be
   * independently accessed; in addition, object identifiers are mapped to a list of their field
   * names to improve deletion performance.
   *
   * @param id Object identifier.
   * @param obj Object to upsert.
   * @param gen Implicit labeled generic for type safety.
   * @param map Implicit mapping of the object to a map.
   * @tparam T Type of object.
   * @tparam R Type of representation.
   */
  def insert[T, R <: HList](id: String, obj: T)(
    implicit gen: LabelledGeneric.Aux[T, R],
    map: ToMap.Aux[R, Symbol, Any]
  ): Unit = {
    require(!id.contains(FieldDelimiter), "Identifier may not contain field delimiter.")

    // Extract the fields of the object
    val fields = map(gen.to(obj)).map { case (sym, value) => sym.name -> value }

    // Insert a mapping between the object's id and the names of its various fields as well as a
    // mapping between each field and its value.
    this.snapshot += id -> fields
    fields.foreach { case (name, value) =>
      this.snapshot += (id + FieldDelimiter + name) -> value
    }
  }

  /**
   * Deletes the object with the provided identifier and all of its various fields. Deletion is
   * guaranteed to be idempotent. It is the responsibility of the caller to ensure that all dangling
   * references to this object are also deleted.
   *
   * @param id Object identifier.
   */
  def delete(id: String): Unit = {
    require(!id.contains(FieldDelimiter), "Identifier may not contain field delimiter.")

    // Extract the names of the object's various fields from its id.
    val names = this.snapshot.get(id) match {
      case Some(n: Iterable[String]) => n
      case _ => Seq.empty
    }

    // Remove the object and its various fields if it exists.
    if (names.nonEmpty) {
      this.snapshot -= id
      names.foreach { name =>
        this.snapshot -= (id + FieldDelimiter + name)
      }
    }
  }

}

object Schema {

  /**
   * Reserved sequence of characters that each [[Schema]] internally uses to differentiate between
   * objects and their various fields. Because object identifiers are guaranteed to be globally
   * unique and may not contain this delimiter, each field identifier formed by concatenating the
   * object identifier, delimiter, and the field name is also guaranteed to be globally unique.
   *
   * Selection of a good field delimiter is a challenging problem. On one hand, you would like to
   * select a delimiter that is long enough that it does not appear frequently in object
   * identifiers. On the other hand, you would also like a delimiter that is short because it takes
   * up less space in memory. After very little deliberation, I decided to use $@|@$.
   */
  val FieldDelimiter = "$@|@$"

}