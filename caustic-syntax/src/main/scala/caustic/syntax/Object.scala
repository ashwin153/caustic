package caustic.syntax

import Object._

/**
 *
 * @param name
 * @param owner
 */
case class Object(name: Transaction, owner: Option[Object]) {

  /**
   *
   * @return
   */
  def key: Transaction = this.owner match {
    case Some(obj) => obj.key ++ FieldDelimiter ++ this.name
    case None => this.name
  }

  /**
   *
   * @return
   */
  def fields: Index = Index(get("$fields"))

  /**
   *
   * @param field
   * @return
   */
  def get(field: Transaction): Object =
    Object(field, Some(this))

  /**
   *
   * @param field
   * @param value
   */
  def set(field: Transaction, value: Transaction)(implicit ctx: Context): Unit = {
    // Verify that the name is recorded on the owner.
    this.owner.foreach { obj =>
      If (!obj.get("$fields").contains(this.name)) {
        obj.set("$fields", obj.get("$fields") ++ this.name ++ ArrayDelimiter)
      }
    }

    // Verify that the field is recorded on the object.
    If (!name.startsWith(InternalDelimiter)) {
      If (!get("$fields").contains(field)) {
        set("$fields", get("$fields") ++ field ++ ArrayDelimiter)
      }
    }

    // Update the value of the field.
    ctx += write(this.key ++ FieldDelimiter ++ field, value)
  }

  /**
   * Idempotent.
   *
   * @return
   */
  def deref: Object = this.owner match {
    case Some(_) => Object(read(this.key), None)
    case None => this
  }

}

object Object {

  // Reserved Delimiters.
  val FieldDelimiter: String = "@"
  val ArrayDelimiter: String = ","
  val InternalDelimiter: String = "$"

}