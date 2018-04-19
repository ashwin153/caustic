package caustic.library.record
package ops

import caustic.library.collection._
import caustic.library.control.Context
import caustic.library.typing._

import shapeless._
import shapeless.ops.hlist.LeftFolder
import shapeless.ops.record.{Keys, Selector}

object move extends Poly2 {

  /**
   * A move argument.
   *
   * @param src Source record.
   * @param dest Destination record.
   */
  case class Args[T](src: Reference[T], dest: Reference[T])

  implicit def caseScalar[
    T,
    TRepr <: HList,
    FieldName <: Symbol,
    FieldType <: Primitive
  ](
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, TRepr],
    selector: Selector.Aux[TRepr, FieldName, FieldType],
    field: Field.Aux[FieldType, Variable[FieldType]]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    field(x.dest, f.name) := field(x.src, f.name)
    x
  }


  implicit def casePointer[
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
    evidence: FieldType <:< Reference[FieldT]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    field(x.dest, f.name).pointer := field(x.src, f.name).pointer
    x
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
    field(x.dest, f.name) := field(x.src, f.name)
    x
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
    field(x.dest, f.name) := field(x.src, f.name)
    x
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
    field(x.dest, f.name) := field(x.src, f.name)
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
    fieldFolder: LeftFolder.Aux[FieldKeys, Args[FieldT], move.type, Args[FieldT]],
    evidence: FieldType <:!< Reference[FieldT]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    field(x.src, f.name).move(field(x.dest, f.name))
    x
  }

}