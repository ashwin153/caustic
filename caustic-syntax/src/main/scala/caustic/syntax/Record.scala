package caustic.syntax

import caustic.syntax.Record._

import scala.language.{dynamics, reflectiveCalls}

/**
 * A dynamically-typed, database object. Records are associated with a key, which uniquely
 * identifies the record in the database. There are three kinds of records: structures, attributes,
 * and references. Records are mutable and transactional.
 *
 * @param key Database key.
 * @param ctx Implicit transaction context.
 */
case class Record(key: Transaction)(implicit ctx: Context) extends Dynamic {

  /**
   * Returns the type of the record. Records may be one of three kinds: structures, attributes, or
   * references. Structures are containers of records, attributes are containers of values, and
   * references are pointers to records.
   *
   * @return Record type.
   */
  def kind: Record =
    Record(this.key ++ FieldDelimiter ++ "kind")

  /**
   * Returns the field of the record with the specified name. Field keys are formed by concatenating
   * the parent record key, a reserved field delimiter ('@'), and the field name. For example,
   * the 'bar' field of record 'foo' would have key 'foo@bar'. Pointer records are automatically
   * dereferenced.
   *
   * @param name Field name.
   * @return Field record.
   */
  def get(name: Transaction): Record =
    Record(branch(this.kind === Reference, read(this.key), this.key) ++ FieldDelimiter ++ name)

  def selectDynamic(name: String): Record =
    // x.foo -> x.get("foo")
    get(name)

  def applyDynamic(name: String)(rest: Transaction*): Record =
    // x.foo("bar", "car") -> x.get("foo").get("bar").get("car")
    rest.foldLeft(get(name))((r, f) => r.get(f))

  /**
   * Updates the kind and value of the field with the specified name.
   *
   * @param name Field name.
   * @param value Updated value.
   * @param kind Updated type.
   */
  def set(name: Transaction, value: Transaction, kind: Transaction): Unit = {
    ctx.set("$key", get(name).key)
    While (ctx.get("$key").contains(FieldDelimiter)) {
      // Extract the parent record key and the field name.
      val delimiter = ctx.get("$key").indexOf(FieldDelimiter)
      val parent = Record(ctx.get("$key").substring(0, delimiter))
      val field = ctx.get("$key").substring(delimiter + FieldDelimiter.length)

      // Verify that the parent record is a structure.
      If (parent.kind <> Structure) {
        ctx += write(parent.kind.key, Structure)
        ctx += write(parent.key, "")
      }

      // Verify that the parent record contains the field name.
      If (!parent.contains(field)) {
        ctx += write(parent.key, parent ++ field ++ ArrayDelimiter)
      }

      // Recurse on the parent record.
      ctx.set("$key", parent.key)
    }

    // Update the kind of the field.
    If (get(name).kind === Structure) {
      get(name).delete()
    }

    If (get(name).kind <> kind) {
      ctx += write(get(name).kind.key, kind)
    }

    // Update the value of the field.
    If (get(name) <> value) {
      ctx += write(get(name).key, value)
    }
  }

  def updateDynamic(field: String)(value: Transaction)(
    implicit ctx: Context
  ): Unit =
    // x.foo = 3 -> x.set("foo", 3, Attribute)
    set(field, value, Attribute)

  def updateDynamic(field: String)(record: Record)(
    implicit ctx: Context
  ): Unit =
    // x.foo = y -> x.set("foo", y.key, Reference)
    // x.foo = y.bar -> x.set("foo", y.bar, Attribute)
    set(field,
      branch(record.kind === Structure, record.key, record),
      branch(record.kind === Structure, Reference, Attribute)
    )

  /**
   * Recursively deletes the record and all its children. Deletion removes the value, but not the
   * version of all deleted keys. This guarantees transactional consistency; however, these
   * "tombstone" records (versions without values) still take up space in underlying database, and
   * it is the responsibility of the user to purge them.
   */
  def delete(): Unit = {
    // Delete the referenced record, or the record itself.
    ctx.set("$keys", branch(
      this.kind === Record.Reference,
      read(this.key) ++ ArrayDelimiter,
      this.key ++ ArrayDelimiter
    ))

    While (ctx.get("$keys") <> Empty) {
      // Pop the head value.
      val until = ctx.get("$keys").indexOf(ArrayDelimiter)
      val head = Record(ctx.get("$keys").substring(0, until))
      ctx.set("$keys", ctx.get("$keys").substring(until + ArrayDelimiter.length))

      If (head.kind === Record.Structure) {
        // Prefix all fields with the head's key.
        ctx.set("$fields", head)
        ctx.set("$i", 0)

        While (ctx.get("$i") < ctx.get("$fields").length) {
          val next = ctx.get("$fields").substring(ctx.get("$i")).indexOf(ArrayDelimiter)
          val name = ctx.get("$fields").substring(ctx.get("$i"), next)

          val prefix = ctx.get("$fields").substring(0, ctx.get("$i"))
          val suffix = ctx.get("$fields").substring(next)
          ctx.set("$fields", prefix ++ head.get(name).key ++ suffix)
          ctx.set("$i", ctx.get("$i") + head.get(name).key.length + ArrayDelimiter.length)
        }

        // Prefetch keys and add them to the list to be processed.
        prefetch(ctx.get("$fields"))
        ctx.set("$keys", ctx.get("$keys") ++ ctx.get("$fields") ++ ArrayDelimiter)
      }

      // Delete the record's value and kind.
      ctx += write(head.kind.key, "")
      ctx += write(head.key, "")
    }
  }

}

object Record {

  // Record kinds.
  val Structure: String = "S"
  val Attribute: String = "A"
  val Reference: String = "R"

  // Reserved Delimiters.
  val FieldDelimiter: String = "@"
  val ArrayDelimiter: String = ","

}