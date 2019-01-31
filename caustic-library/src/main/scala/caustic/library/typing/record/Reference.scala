package caustic.library
package typing
package record

import caustic.library.typing.Value._
import caustic.runtime.Program
import shapeless._
import shapeless.ops.hlist.LeftFolder
import shapeless.ops.record.{Keys, Selector}

/**
 * A record reference.
 *
 * @param pointer Underlying pointer.
 */
case class Reference[T](pointer: Variable[String]) extends Internal {

  /**
   * Returns the address of the reference.
   *
   * @return Referenced address.
   */
  def key: Value[String] = this.pointer.get

  /**
   * Returns a container that stores the value of the attribute with the specified name.
   *
   * @param witness Attribute name.
   * @param gen Generic representation.
   * @param selector Attribute selector.
   * @param field Field converter.
   * @return Container for the value of the field.
   */
  def get[Repr <: HList, Name <: Symbol, Type, Container](witness: Witness.Lt[Name])(
    implicit gen: LabelledGeneric.Aux[T, Repr],
    selector: Selector.Aux[Repr, Name, Type],
    field: Field.Aux[Type, Container]
  ): Container = field(this.pointer, witness.value.name)

  /**
   * Deletes all attributes of the record.
   *
   * @param recursive Recursively delete referenced records.
   * @param context Parse context.
   * @param generic Generic representation.
   * @param keys Attribute names.
   * @param folder Attribute iterator.
   */
  def delete[Repr <: HList, KeysRepr <: HList](recursive: scala.Boolean = false)(
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, Repr],
    keys: Keys.Aux[Repr, KeysRepr],
    folder: LeftFolder.Aux[KeysRepr, ops.delete.Args[T], ops.delete.type, ops.delete.Args[T]]
  ): Unit = keys().foldLeft(ops.delete.Args(this, recursive))(ops.delete)

  /**
   * Copies all attributes of this record to the destination record.
   *
   * @param destination Destination record.
   * @param context Parse context.
   * @param generic Generic representation.
   * @param keys Attribute names.
   * @param folder Attribute iterator.
   */
  def move[Repr <: HList, KeysRepr <: HList](destination: Reference[T])(
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, Repr],
    keys: Keys.Aux[Repr, KeysRepr],
    folder: LeftFolder.Aux[KeysRepr, ops.move.Args[T], ops.move.type, ops.move.Args[T]]
  ): Unit = keys().foldLeft(ops.move.Args(this, destination))(ops.move)

  /**
   * Returns whether or not the references are equal.
   *
   * @param that Another reference.
   * @param context Parse context.
   * @param generic Generic representation.
   * @param keys Attribute names.
   * @param folder Attribute iterator.
   * @return Whether or not the references are equal.
   */
  def ===[Repr <: HList, KeysRepr <: HList](that: Reference[T])(
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, Repr],
    keys: Keys.Aux[Repr, KeysRepr],
    folder: LeftFolder.Aux[KeysRepr, ops.equal.Args[T], ops.equal.type, ops.equal.Args[T]]
  ): Value[Boolean] = keys().foldLeft(ops.equal.Args(this, that, false))(ops.equal).equals

  /**
   * Serializes all attributes of the record to JSON.
   *
   * @param context Parse context.
   * @param generic Generic representation.
   * @param keys Attribute names.
   * @param folder Attribute iterator.
   */
  def asJson[Repr <: HList, KeysRepr <: HList](
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, Repr],
    keys: Keys.Aux[Repr, KeysRepr],
    folder: LeftFolder.Aux[KeysRepr, ops.json.Args[T], ops.json.type, ops.json.Args[T]]
  ): Value[String] = {
    val json = convert("{\"key\": ") + this.pointer.key.quoted
    json + keys().foldLeft(ops.json.Args(this, ""))(ops.json).json + "}"
  }

}

object Reference {

  /**
   * Returns a reference to the specified local variable.
   *
   * @param key Local variable.
   * @return
   */
  def Local[T](key: Value[String]): Reference[T] = Reference(Variable.Local(key))

  /**
   * Returns a reference to the specified remote variable.
   *
   * @param key Remote variable.
   * @return Remote reference.
   */
  def Remote[T](key: Value[String]): Reference[T] = Reference(Variable.Remote(key))

  // Implicit Operations.
  implicit class AssignmentOps[T](x: Reference[T]) {
    def :=[U](y: Pointer[U])(implicit context: Context): Unit = x.pointer.set(y.key)
    def :=[Repr <: HList, KeysRepr <: HList](y: Reference[T])(
      implicit context: Context,
      generic: LabelledGeneric.Aux[T, Repr],
      keys: Keys.Aux[Repr, KeysRepr],
      folder: LeftFolder.Aux[KeysRepr, ops.move.Args[T], ops.move.type, ops.move.Args[T]]
    ): Unit = if (x.pointer.getClass == y.pointer.getClass) x.pointer := y.pointer else y.move(x)
  }

}