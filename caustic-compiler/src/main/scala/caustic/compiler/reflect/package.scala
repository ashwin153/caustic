package caustic.compiler

package object reflect {

  /**
   * Returns the least upper bound of the specified simple types.
   *
   * @param x A type.
   * @param y Another type.
   * @return Least upper bound.
   */
  def lub(x: Type, y: Type): Type = (x, y) match {
    case (a, b) if a == b                  => x
    case (_: CFunction, _) | (_, _: CFunction) => CUnit
    case (_: CPointer , _) | (_, _: CPointer ) => CUnit
    case (_: CStruct  , _) | (_, _: CStruct  ) => CUnit
    case (CUnit      , _) | (_, CUnit      ) => CUnit
    case (CString    , _) | (_, CString    ) => CString
    case (CDouble    , _) | (_, CDouble    ) => CDouble
    case (CInt       , _) | (_, CInt       ) => CInt
    case (CBoolean   , _) | (_, CBoolean   ) => CBoolean
  }

  /**
   * Returns the greatest lower bound of the specified simple types.
   *
   * @param x A type.
   * @param y Another type.
   * @return Greatest lower bound.
   */
  def glb(x: Type, y: Type): Type = (x, y) match {
    case (a, b) if a == b                    => x
    case (_: CSet    , _) | (_, _: CSet    ) => CUnit
    case (_: CMap    , _) | (_, _: CMap    ) => CUnit
    case (_: CFunction, _) | (_, _: CFunction) => CUnit
    case (_: CPointer , _) | (_, _: CPointer ) => CUnit
    case (_: CStruct  , _) | (_, _: CStruct  ) => CUnit
    case (CBoolean   , _) | (_, CBoolean   ) => CBoolean
    case (CInt       , _) | (_, CInt       ) => CInt
    case (CDouble    , _) | (_, CDouble    ) => CDouble
    case (CString    , _) | (_, CString    ) => CString
    case (CUnit      , _) | (_, CUnit      ) => CUnit
  }

  /**
   * Returns whether or not x is a subtype of the specified type.
   *
   * @param x A type.
   * @param of Another type.
   * @return Whether or not x is a subtype.
   */
  def isSubtype(x: Type, of: Type): Boolean =
    lub(x, of) == of

  /**
   * Returns whether or not x is a supertype of the specified type.
   *
   * @param x A type.
   * @param of Another type.
   * @return Whether or not x is a supertype.
   */
  def isSupertype(x: Type, of: Type): Boolean =
    glb(x, of) == of

  /**
   * Returns whether or not x is within the specified type bounds.
   *
   * @param x A type.
   * @param lower Lower bound.
   * @param upper Upper bound.
   * @return Whether or not x is in the bounds.
   */
  def isBetween(x: Type, lower: Type, upper: Type): Boolean =
    isSupertype(x, lower) && isSubtype(x, upper)

  /**
   * Returns whether or not x corresponds to a numeric type.
   *
   * @param x A type.
   * @return Whether or not x is numeric.
   */
  def isNumeric(x: Type): Boolean =
    isBetween(x, CInt, CDouble)

}
