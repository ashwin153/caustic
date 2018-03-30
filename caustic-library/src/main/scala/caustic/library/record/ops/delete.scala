package caustic.library.record.ops

import caustic.library.control.Context
import caustic.library.record.{Field, Reference}
import caustic.library.typing.{Primitive, Variable}
import caustic.runtime.Null

import shapeless.{HList, LabelledGeneric, Poly2}
import shapeless.ops.hlist.LeftFolder
import shapeless.ops.record.{Keys, Selector}

object delete extends Poly2 {

  /**
   * A delete argument.
   *
   * @param src Source reference.
   * @param recursive Recursively delete referenced records.
   */
  case class Args[T](src: Reference[T], recursive: scala.Boolean)

  implicit def caseScalar[T,
    TRepr <: HList,
    FieldName <: Symbol,
    FieldType <: Primitive
  ](
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, TRepr],
    selector: Selector.Aux[TRepr, FieldName, FieldType],
    field: Field.Aux[FieldType, Variable[FieldType]]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    field(x.src, f.name).set(Null)
    x
  }

  implicit def casePointer[
    T,
    TRepr <: HList,
    FieldName <: Symbol,
    FieldT,
    FieldRepr <: HList,
    FieldKeys <: HList
  ](
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, TRepr],
    selector: Selector.Aux[TRepr, FieldName, Reference[FieldT]],
    field: Field.Aux[Reference[FieldT], Reference[FieldT]],
    fieldGeneric: LabelledGeneric.Aux[FieldT, FieldRepr],
    fieldKeys: Keys.Aux[FieldRepr, FieldKeys],
    fieldFolder: LeftFolder.Aux[FieldKeys, Args[FieldT], delete.type, Args[FieldT]]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    if (x.recursive) field(x.src, f.name).delete(x.recursive)
    x.src.key.set(Null)
    x
  }

  implicit def caseNested[
    T,
    TRepr <: HList,
    FieldName <: Symbol,
    FieldType,
    FieldT,
    FieldRepr <: HList,
    FieldKeys <: HList
  ](
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, TRepr],
    selector: Selector.Aux[TRepr, FieldName, FieldType],
    field: Field.Aux[FieldType, Reference[FieldT]],
    fieldGeneric: LabelledGeneric.Aux[FieldT, FieldRepr],
    fieldKeys: Keys.Aux[FieldRepr, FieldKeys],
    fieldFolder: LeftFolder.Aux[FieldKeys, Args[FieldT], delete.type, Args[FieldT]]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    field(x.src, f.name).delete(x.recursive)
    x
  }

}