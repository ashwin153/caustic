package com.schema.objects

import shapeless._
import shapeless.labelled._
import shapeless.ops.record._
import Schema._

/**
 * A strongly-typed, object-oriented interface over a snapshot. Schemas store objects and their
 * various fields as independent entries in an underlying snapshot. Accesses and modifications to
 * objects are made through a [[Proxy]] which is backed by the underlying snapshot. Therefore,
 * (1) the underlying snapshot contains the only copy of each object and field and (2) every access
 * and modification to any field of any object is performed directly on the underlying snapshot.
 * Because schemas rely on [[Proxy]] to ensure static type-safety, they may only be used for types
 * for which a [[LabelledGeneric]] representation can be implicitly materialized (ie. case classes).
 *
 * @param snapshot Underlying snapshot.
 */
case class Schema(snapshot: Snapshot) {

  /**
   * Selects the object with the specified identifier and type. Returns 'None' if the object does
   * not exist in the snapshot, or returns a [[Proxy]] object backed this schema. In other words,
   * field-level accesses and modifications to the proxy object are fulfilled by the underlying
   * snapshot itself. Calling get on a [[Proxy]] to a deleted object will throw a
   * [[NoSuchElementException]] and calling set will have no effect.
   *
   * @param id Object identifier.
   * @param gen Implicit labled generic for type safety.
   * @tparam T Type of object.
   * @return Proxy object, or None if the identifier doesn't exist.
   */
  def select[T](id: String)(implicit gen: LabelledGeneric[T]): Option[Proxy[T]] = {
    require(!id.contains(FieldDelimiter), "Identifier may not contain field delimiter.")

    this.snapshot.get(id).map(_ => new Proxy[T] {

      override def get[R <: HList, K <: Symbol, V](witness: Witness.Lt[K])(
        implicit gen: LabelledGeneric.Aux[T, R],
        selector: Selector.Aux[R, K, V]
      ): V =
        snapshot(id + FieldDelimiter + witness.value.name).asInstanceOf[V]

      override def set[R <: HList, K <: Symbol, V](witness: Witness.Lt[K], value: V)(
        implicit gen: LabelledGeneric.Aux[T, R],
        updater: Updater.Aux[R, FieldType[K, V], R]
      ): Unit =
        if (snapshot.contains(id + FieldDelimiter + witness.value.name))
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
   * @param extract Implicit mapping of the object to a map.
   * @tparam T Type of object.
   * @tparam R Type of representation.
   */
  def insert[T, R <: HList](id: String, obj: T)(
    implicit gen: LabelledGeneric.Aux[T, R],
    extract: ToMap[R]
  ): Unit = {
    require(!id.contains(FieldDelimiter), "Identifier may not contain field delimiter.")

    // Extract the fields of the object
    val fields = extract(gen.to(obj)) collect { case (sym: Symbol, value) => sym.name -> value }

    // Insert a mapping between the object's id and the names of its various fields as well as a
    // mapping between each field and its value.
    this.snapshot += id -> fields.keys
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
    val fields = this.snapshot.get(id) match {
      case Some(n) => n.asInstanceOf[Iterable[String]]
      case _ => Seq.empty
    }

    // Remove the object and its various fields if it exists.
    if (fields.nonEmpty) {
      this.snapshot -= id
      fields.foreach { field =>
        this.snapshot -= (id + FieldDelimiter + field)
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