package caustic.library.record.ops

import caustic.library.collection.{List, Map, Set}
import caustic.library.control.Context
import caustic.library.record.{Field, Reference}
import caustic.library.typing._
import caustic.library.typing.Value._

import shapeless._
import shapeless.ops.hlist.LeftFolder
import shapeless.ops.record.{Keys, Selector}

object json extends Poly2 {

  /**
   * A JSON serializer argument.
   *
   * @param src Source reference.
   * @param json JSON string.
   */
  case class Args[T](src: Reference[T], json: Value[String])

  implicit def caseScalar[
    T,
    TRepr <: HList,
    FieldName <: Symbol,
    FieldType <: Double
  ](
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, TRepr],
    selector: Selector.Aux[TRepr, FieldName, FieldType],
    field: Field.Aux[FieldType, Variable[FieldType]]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(json = x.json + ", \"" + f.name + "\": " + field(x.src, f.name).asJson)
  }

  implicit def caseString[
    T,
    TRepr <: HList,
    FieldName <: Symbol,
    FieldType
  ](
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, TRepr],
    selector: Selector.Aux[TRepr, FieldName, FieldType],
    field: Field.Aux[FieldType, Variable[String]],
    evidence: String <:< FieldType <:< Primitive
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(json = x.json + ", \"" + f.name + "\": " + field(x.src, f.name).asJson)
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
    x.copy(json = x.json + ", \"" + f.name + "\": " + field(x.src, f.name).asJson)
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
    x.copy(json = x.json + ", \"" + f.name + "\": " + field(x.src, f.name).asJson)
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
    x.copy(json = x.json + ", \"" + f.name + "\": " + field(x.src, f.name).asJson)
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
    fieldGeneric: LabelledGeneric.Aux[FieldT, FieldRepr],
    fieldKeys: Keys.Aux[FieldRepr, FieldKeys],
    fieldFolder: LeftFolder.Aux[FieldKeys, Args[FieldT], json.type, Args[FieldT]],
    evidence: FieldType <:< Reference[FieldT]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(json = x.json + ", \"" + f.name + "\": \"" + field(x.src, f.name).key + "\"")
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
    fieldFolder: LeftFolder.Aux[FieldKeys, Args[FieldT], json.type, Args[FieldT]],
    evidence: FieldType <:!< Reference[FieldT]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(json = x.json + ", \"" + f.name + "\": " + field(x.src, f.name).asJson)
  }

}