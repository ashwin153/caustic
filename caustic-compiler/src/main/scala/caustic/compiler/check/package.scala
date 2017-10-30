package caustic.compiler

package object check {

  /**
   * Returns the least upper bound of the specified types.
   *
   * @param x Type.
   * @param y Type.
   * @return Least upper bound of types.
   */
  def lub(x: Type, y: Type): Type = (x, y) match {
    case (Textual, _) | (_, Textual) => Textual
    case (Decimal, _) | (_, Decimal) => Decimal
    case (Integer, _) | (_, Integer) => Integer
    case (Boolean, _) | (_, Boolean) => Boolean
  }

}
