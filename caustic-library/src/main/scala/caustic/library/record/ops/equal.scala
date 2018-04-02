package caustic.library.record.ops

import caustic.library.control.Context
import caustic.library.record.{Field, Reference}
import caustic.library.typing._

import shapeless.ops.hlist.LeftFolder
import shapeless.ops.record.{Keys, Selector}
import shapeless.{HList, LabelledGeneric, Poly2}

object equal extends Poly2 {

   /**
   * An equal argument.
   *
   * @param a A reference.
   * @param b Another reference.
   */
  case class Args[T](a: Reference[T], b: Reference[T], equals: Value[Boolean])

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
    x.copy(equals = x.equals && field(x.a, f.name) === field(x.b, f.name))
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
    fieldFolder: LeftFolder.Aux[FieldKeys, Args[FieldT], equal.type, Args[FieldT]]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(equals = x.equals && field(x.a, f.name).key === field(x.b, f.name).key)
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
    fieldFolder: LeftFolder.Aux[FieldKeys, Args[FieldT], equal.type, Args[FieldT]]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(equals = x.equals && field(x.a, f.name) === field(x.b, f.name))
  }

}
