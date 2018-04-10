package caustic.compiler

package object typing {

  /**
   * Returns the least upper bound of the specified types.
   *
   * @param x A type.
   * @param y Another type.
   * @return Least upper bound.
   */
  def lub(x: Kind, y: Kind): Kind = (x, y) match {
    case (Pointer(k), Pointer(l)) if k == l => Pointer(k)
    case (Struct(s, f), Struct(t, g)) if s == t && f == g => Struct(s, f)
    case (CUnit,    _) | (_, CUnit) => CUnit
    case (CString,  _) | (_, CString) => CString
    case (CDouble,  _) | (_, CDouble) => CDouble
    case (CInt,     _) | (_, CInt) => CInt
    case (CBoolean, _) | (_, CBoolean) => CBoolean
    case _ => CUnit
  }

  /**
   * Returns the least upper bound of the specified types.
   *
   * @param x A type.
   * @param y Another type.
   * @param z Additional types.
   * @return Least upper bound.
   */
  def lub(x: Kind, y: Kind, z: Kind*): Kind = z.foldLeft(lub(x, y))(lub)

}
