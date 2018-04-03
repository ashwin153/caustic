package caustic.library.record.ops

import caustic.library.control.Context
import caustic.library.record.{Field, Reference}
import caustic.library.typing._
import caustic.runtime._
import shapeless._
import shapeless.ops.hlist.LeftFolder
import shapeless.ops.record.{Keys, Selector}

object json extends Poly2 {

  /**
   * A JSON serializer argument.
   *
   * @param src Source reference.
   * @param json JSON string.
   * @param recursive Recursively serialize referenced records.
   */
  case class Args[T](src: Reference[T], json: Value[String], recursive: scala.Boolean)

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
    val json = branch(field(x.src, f.name) <> Null, field(x.src, f.name), "null")
    x.copy(json = x.json ++ ", \"" ++ f.name ++ "\": " ++ json)
  }

  implicit def caseString[
    T,
    TRepr <: HList,
    FieldName <: Symbol
  ](
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, TRepr],
    selector: Selector.Aux[TRepr, FieldName, String],
    field: Field.Aux[String, Variable[String]]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    val json = branch(field(x.src, f.name) <> Null, field(x.src, f.name).quoted, "null")
    x.copy(json = x.json ++ ", \"" ++ f.name ++ "\": " ++ json)
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
    val json = if (x.recursive) field(x.src, f.name).json() else field(x.src, f.name).key
    x.copy(json = x.json ++ ", \"" ++ f.name ++ "\": \"" ++ json ++ "\"")
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
    x.copy(json = x.json ++ ", \"" ++ f.name ++ "\": " ++ field(x.src, f.name).json())
  }

}