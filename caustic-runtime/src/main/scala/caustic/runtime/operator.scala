package caustic.runtime

/**
 * An enumeration of the various transformations permitted in an expressions.
 */
sealed trait Operator
case object Add       extends Operator  // Returns (1) plus (2).
case object Both      extends Operator  // Returns (1) && (2).
case object Branch    extends Operator  // Evaluate (2) if (1) or (3) otherwise.
case object Cons      extends Operator  // Evaluate (1) and then returns (2).
case object Contains  extends Operator  // Returns whether (2) is contained in (1).
case object Cos       extends Operator  // Returns the cosine of (1) in radians.
case object Div       extends Operator  // Returns (1) divided by (2).
case object Either    extends Operator  // Returns (1) || (2).
case object Equal     extends Operator  // Returns whether (1) equals (2).
case object Floor     extends Operator  // Returns the floor of (1).
case object IndexOf   extends Operator  // Returns the index of (2) in (1).
case object Length    extends Operator  // Returns the string length of (1).
case object Less      extends Operator  // Returns whether (1) is less than (2).
case object Load      extends Operator  // Returns the value of variable (1).
case object Log       extends Operator  // Returns the natural logarithm of (1).
case object Matches   extends Operator  // Returns whether (1) matches regex (2).
case object Mod       extends Operator  // Retu rns (1) mod (2).
case object Mul       extends Operator  // Returns (1) times (2).
case object Negate    extends Operator  // Returns the negation of (1).
case object Pow       extends Operator  // Returns (1) raised to the (2).
case object Prefetch  extends Operator  // Reads keys at (1)/i for i in [0, (2)).
case object Read      extends Operator  // Returns the value of key (1).
case object Repeat    extends Operator  // Evaluate (2) until (1) is not satisfied.
case object Rollback  extends Operator  // Discard changes and return (1).
case object Sin       extends Operator  // Returns the sine of (1) in radians.
case object Slice     extends Operator  // Returns the substring of (1) in [2, 3).
case object Store     extends Operator  // Sets the value of variable (1) to (2).
case object Sub       extends Operator  // Returns (1) minus (2).
case object Write     extends Operator  // Sets the value of key (1) to (2).