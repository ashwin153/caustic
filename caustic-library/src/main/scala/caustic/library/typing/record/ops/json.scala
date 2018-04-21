package caustic.library.typing
package record
package ops

import caustic.library.Context
import caustic.library.typing.collection._
import caustic.library.typing.Value._

import shapeless._
import shapeless.ops.hlist.LeftFolder
import shapeless.ops.record._

object json extends Poly2 {

  /**
   * A JSON serializer argument.
   *
   * @param src Source reference.
   * @param json JSON string.
   */
  case class Args[T](src: Reference[T], json: Value[String])

  implicit def caseString[
    T,
    TRepr <: HList,
    FieldName <: Symbol,
    FieldType,
    FieldT >: String <: Primitive
  ](
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, TRepr],
    selector: Selector.Aux[TRepr, FieldName, FieldType],
    field: Field.Aux[FieldType, Variable[FieldT]],
    evidence: FieldType <:< Value[FieldT]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(json = x.json + ", \"" + f.name + "\": " + field(x.src, f.name).asJson)
  }

  implicit def caseValue[
    T,
    TRepr <: HList,
    FieldName <: Symbol,
    FieldType,
    FieldT <: Double
  ](
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, TRepr],
    selector: Selector.Aux[TRepr, FieldName, FieldType],
    field: Field.Aux[FieldType, Variable[FieldT]],
    evidence: FieldType <:< Value[FieldT]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(json = x.json + ", \"" + f.name + "\": " + field(x.src, f.name).asJson)
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
    fieldFolder: LeftFolder.Aux[FieldKeys, Args[FieldT], json.type, Args[FieldT]],
    evidence: FieldType <:< Reference[FieldT]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(json = x.json + ", \"" + f.name + "\": \"" + field(x.src, f.name).key + "\"")
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
    fieldFolder: LeftFolder.Aux[FieldKeys, Args[FieldT], json.type, Args[FieldT]],
    evidence: FieldType <:!< Reference[FieldT]
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
    field: Field.Aux[FieldType, collection.List[FieldValue]],
    evidence: FieldType <:< collection.List[FieldValue]
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
    field: Field.Aux[FieldType, collection.Map[FieldKey, FieldValue]],
    evidence: FieldType <:< collection.Map[FieldKey, FieldValue]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(json = x.json + ", \"" + f.name + "\": " + field(x.src, f.name).asJson)
  }

}