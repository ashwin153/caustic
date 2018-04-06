package caustic.library

import caustic.library.control.Context

import shapeless.{HList, LabelledGeneric}
import shapeless.ops.hlist.LeftFolder
import shapeless.ops.record.Keys

package object record {

  // Implicit Operations.
  implicit class AssignmentOps[T](x: Reference[T]) {
    def :=[Repr <: HList, KeysRepr <: HList](y: Reference[T])(
      implicit context: Context,
      generic: LabelledGeneric.Aux[T, Repr],
      keys: Keys.Aux[Repr, KeysRepr],
      folder: LeftFolder.Aux[KeysRepr, ops.move.Args[T], ops.move.type, ops.move.Args[T]]
    ): Unit = y.move(x)
  }

}