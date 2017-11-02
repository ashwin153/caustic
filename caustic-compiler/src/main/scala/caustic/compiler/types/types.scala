package caustic.compiler

package object types {

  /**
   * Returns the least upper bound of the specified types, or Null if the types are incomparable.
   * Record types are structurally typed; therefore, the least upper bound of two records is the
   * subset of fields that they have in common. Primitive types conform to the following hierarchy:
   * String << Double << Int << Boolean.
   *
   * @param x First type.
   * @param y Second type.
   * @return Least upper bound.
   */
  def lub(x: types.Type, y: types.Type): types.Type = (x, y) match {
    case (types.Pointer(u), types.Pointer(v))    => types.Pointer(lub(u, v).asInstanceOf[Simple])
    case (types.Record(u), types.Record(v))      => types.Record(u.toSet.intersect(v.toSet).toMap)
    case (types.Null, _)    | (_, types.Null)    => types.Null
    case (types.String, _)  | (_, types.String)  => types.String
    case (types.Double, _)  | (_, types.Double)  => types.Double
    case (types.Int, _)     | (_, types.Int)     => types.Int
    case (types.Boolean, _) | (_, types.Boolean) => types.Boolean
    case _                                       => types.Null
  }

}
