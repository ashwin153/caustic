package caustic.library.record.ops

import caustic.library.control.Context
import caustic.library.record.{Field, Reference}
import caustic.library.typing._

import shapeless.{HList, LabelledGeneric, Poly2}
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

  implicit def caseBoolean[
    T,
    TRepr <: HList,
    FieldName <: Symbol
  ](
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, TRepr],
    selector: Selector.Aux[TRepr, FieldName, Boolean],
    field: Field.Aux[Boolean, Variable[Boolean]]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(json = x.json ++ ", \"" ++ f.name ++ "\": " ++ field(x.src, f.name))
  }

  implicit def caseInt[
    T,
    TRepr <: HList,
    FieldName <: Symbol
  ](
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, TRepr],
    selector: Selector.Aux[TRepr, FieldName, Int],
    field: Field.Aux[Int, Variable[Int]]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(json = x.json ++ ", \"" ++ f.name ++ "\": " ++ field(x.src, f.name))
  }

  implicit def caseDouble[
    T,
    TRepr <: HList,
    FieldName <: Symbol
  ](
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, TRepr],
    selector: Selector.Aux[TRepr, FieldName, Double],
    field: Field.Aux[Double, Variable[Double]]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(json = x.json ++ ", \"" ++ f.name ++ "\": " ++ field(x.src, f.name))
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
    x.copy(json = x.json ++ ", \"" ++ f.name ++ "\": \"" ++ field(x.src, f.name) ++ "\"")
  }

  implicit def caseNull[
    T,
    TRepr <: HList,
    FieldName <: Symbol
  ](
    implicit context: Context,
    generic: LabelledGeneric.Aux[T, TRepr],
    selector: Selector.Aux[TRepr, FieldName, Double],
    field: Field.Aux[Double, Variable[Double]]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    x.copy(json = x.json ++ ", \"" ++ f.name ++ "\": null")
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
    fieldFolder: LeftFolder.Aux[FieldKeys, Args[FieldT], json.type, Args[FieldT]]
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
    fieldFolder: LeftFolder.Aux[FieldKeys, Args[FieldT], json.type, Args[FieldT]]
  ): Case.Aux[Args[T], FieldName, Args[T]] = at[Args[T], FieldName] { (x, f) =>
    val json = field(x.src, f.name).json()
    x.copy(json = x.json ++ "{\"key\": \"" ++ field(x.src, f.name).key ++ "\"" ++ json ++ "}")
  }

}