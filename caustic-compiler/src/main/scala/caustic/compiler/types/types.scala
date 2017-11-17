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
  def lub(x: Type, y: Type): Type = (x, y) match {
    case (Pointer(u), Pointer(v))        => Pointer(lub(u, v).asInstanceOf[Simple])
    case (Pointer(u), v)                 => Pointer(lub(u, v).asInstanceOf[Simple])
    case (u, Pointer(v))                 => Pointer(lub(u, v).asInstanceOf[Simple])
    case (Record(u), Record(v))          => Record(u.toSet.intersect(v.toSet).toMap)
    case (Null, _)      | (_, Null)      => Null
    case (String, _)    | (_, String)    => String
    case (Double, _)    | (_, Double)    => Double
    case (Int, _)       | (_, Int)       => Int
    case (Boolean, _)   | (_, Boolean)   => Boolean
    case _                               => Null
  }

}
