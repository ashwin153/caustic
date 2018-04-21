package caustic.library.typing
package record
package ops

import caustic.library.Context
import caustic.library.typing.collection._
import caustic.library.typing.Value._

import shapeless._
import shapeless.ops.hlist.LeftFolder
import shapeless.ops.record.{Keys, Selector}

object equal extends Poly2 {

   /**
   * An equal argument.
   *
   * @param a A reference.
   * @param b Another reference.
   */
  case class Args[T](a: Reference[T], b: Reference[T], equals: Value[Boolean])

  implicit def caseValue[T,
    TRepr <: HList,
    FieldName <: Symbol,
    FieldType,
    FieldT <: Primitive
  ](
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, TRepr],
    selector: Selector.Aux[TRepr, FieldName, FieldType],
    field: Field.Aux[FieldType, Variable[FieldT]],
    evidence: FieldType <:< Value[FieldT]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(equals = x.equals && field(x.a, f.name) === field(x.b, f.name))
  }

  implicit def caseReference[
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
    fieldFolder: LeftFolder.Aux[FieldKeys, Args[FieldT], equal.type, Args[FieldT]],
    evidence: FieldType <:< Reference[FieldT]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(equals = x.equals && field(x.a, f.name).pointer === field(x.b, f.name).pointer)
  }

  implicit def caseRecord[
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
    fieldFolder: LeftFolder.Aux[FieldKeys, Args[FieldT], equal.type, Args[FieldT]],
    evidence: FieldType <:!< Reference[FieldT]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(equals = x.equals && field(x.a, f.name) === field(x.b, f.name))
  }

  implicit def caseList[
    T,
    TRepr <: HList,
    FieldName <: Symbol,
    FieldType,
    FieldValue <: Primitive
  ](
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, TRepr],
    selector: Selector.Aux[TRepr, FieldName, FieldType],
    field: Field.Aux[FieldType, List[FieldValue]],
    evidence: FieldType <:< List[FieldValue]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(equals = x.equals && field(x.a, f.name) === field(x.b, f.name))
  }

  implicit def caseSet[
    T,
    TRepr <: HList,
    FieldName <: Symbol,
    FieldType,
    FieldValue <: Primitive
  ](
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, TRepr],
    selector: Selector.Aux[TRepr, FieldName, FieldType],
    field: Field.Aux[FieldType, Set[FieldValue]],
    evidence: FieldType <:< Set[FieldValue]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(equals = x.equals && field(x.a, f.name) === field(x.b, f.name))
  }

  implicit def caseMap[
    T,
    TRepr <: HList,
    FieldName <: Symbol,
    FieldType,
    FieldKey <: String,
    FieldValue <: Primitive
  ](
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, TRepr],
    selector: Selector.Aux[TRepr, FieldName, FieldType],
    field: Field.Aux[FieldType, Map[FieldKey, FieldValue]],
    evidence: FieldType <:< Map[FieldKey, FieldValue]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(equals = x.equals && field(x.a, f.name) === field(x.b, f.name))
  }

}
