package schema.runtime
package syntax

import scala.language.dynamics

/**
 * A dynamic delegate type.
 */
sealed trait Proxy extends Dynamic {

  /**
   * Database key associated with the proxy.
   *
   * @return Database key.
   */
  def key: Transaction

  /**
   * Returns the field with the specified name.
   *
   * @param name Field name.
   * @return Corresponding field.
   */
  def selectDynamic(name: String): Field

  /**
   * Returns the value at the specified address of the index with the specified name.
   *
   * @param name Index name.
   * @param address Index address.
   * @return Value at index address.
   */
  def applyDynamic(name: String)(address: Transaction): Field

  /**
   * Updates the field specified by name to the specified value.
   *
   * @param name Field name.
   * @param value Updated value.
   * @param ctx Implicit transaction context.
   */
  def updateDynamic(name: String)(value: Transaction)(implicit ctx: Context): Unit

}

/**
 * A dynamically-typed object proxy.
 *
 * @param key Corresponding database key.
 */
final case class Object(key: Transaction) extends Proxy {

  override def selectDynamic(field: String): Field =
    Field(this.key ++ FieldDelimiter ++ literal(field), field, this)

  override def applyDynamic(field: String)(address: Transaction): Field =
    Field(this.key ++ FieldDelimiter ++ literal(field) ++ FieldDelimiter ++ address, field, this)

  override def updateDynamic(field: String)(value: Transaction)(
    implicit ctx: Context
  ): Unit = {
    // Verify that the owning object exists.
    If (!equal(this, Literal.True)) {
      ctx += write(this.key, Literal.True)
    }

    // Verify that the field name is recorded on the object.
    if (!field.startsWith(LocalDelimiter)) {
      val names = this.key ++ FieldDelimiter ++ "$fields"
      If (!read(names).contains(field)) {
        ctx += write(names, read(names) ++ literal(field) ++ ArrayDelimiter)
      }
    }

    // Append the field update to the context.
    val path = this.key ++ FieldDelimiter ++ literal(field)
    ctx += write(path, value)
  }

}

/**
 * A dynamically-typed field proxy.
 *
 * @param key Corresponding database key.
 * @param name Field name.
 * @param owner Owning object.
 */
final case class Field(key: Transaction, name: String, owner: Object) extends Proxy {

  override def selectDynamic(field: String): Field =
    Field(read(this.key) ++ FieldDelimiter ++ literal(field), field, Object(read(this.key)))

  override def applyDynamic(field: String)(address: Transaction): Field =
    Field(read(this.key) ++ FieldDelimiter ++ literal(field) ++ FieldDelimiter ++ address, field, Object(read(this.key)))

  override def updateDynamic(field: String)(value: Transaction)(
    implicit ctx: Context
  ): Unit = {
    // Verify that the owning object exists.
    If (this.owner === Literal.Empty) {
      ctx += write(this.owner.key, this.owner.key)
    }

    // Verify that the field name is recorded on the owner object.
    if (!field.startsWith(LocalDelimiter)) {
      val names = this.owner.key ++ FieldDelimiter ++ "$fields"
      If (!read(names).contains(field)) {
        ctx += write(names, read(names) ++ literal(field) ++ ArrayDelimiter)
      }
    }

    // Append the field update to the context.
    val path = read(this.key) ++ FieldDelimiter ++ literal(field)
    ctx += write(path, value)
  }

  /**
   * Updates the specified index address to the specified value.
   *
   * @param address Index address.
   * @param value Updated value.
   * @param ctx Implicit transaction context.
   */
  def update(address: Transaction, value: Transaction)(
    implicit ctx: Context
  ): Unit = {
    // Append the array update to the context.
    val path = this.key ++ FieldDelimiter ++ address
    ctx += write(path, value)

    // Verify that the owning object exists.
    If (this.owner === Literal.Empty) {
      ctx += write(this.owner.key, this.owner.key)
    }

    // Verify that the index name is recorded on the owner object.
    val names = this.owner.key ++ FieldDelimiter ++ "$indices"
    If (!read(names).contains(this.name)) {
      ctx += write(names, read(names) ++ literal(this.name) ++ ArrayDelimiter)
    }

    // Verify that the at is recorded on the index.
    val index = this.key ++ FieldDelimiter ++ "$addresses"
    If (!read(index).contains(address)) {
      ctx += write(index, read(index) ++ address ++ ArrayDelimiter)
    }
  }

}
