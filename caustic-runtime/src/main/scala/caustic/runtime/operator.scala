package caustic.runtime

/**
 * An enumeration of the various operators permitted in an [[Expression]].
 */
sealed trait Operator
case object Read      extends Operator  // Returns the value of key (1).            (Text) => Any
case object Write     extends Operator  // Sets the value of key (1) to (2).        (Text, Any) => Null
case object Load      extends Operator  // Returns the value of variable (1).       (Text) => Any
case object Store     extends Operator  // Sets the value of variable (1) to (2).   (Text, Any) => Null
case object Cons      extends Operator  // Evaluate (1) and then returns (2).       (Any, Any) => Any
case object Repeat    extends Operator  // Evaluate (2) until (1) is not satisfied. (Flag, Any) => Null
case object Branch    extends Operator  // Evaluate (2) if (1) or (3) otherwise.    (Flag, Any, Any) => Any
case object Rollback  extends Operator  // Discard changes and return (1).          (Any) => Any
case object Random    extends Operator  // Returns a uniformly random on [0, 1).    () => Real
case object Add       extends Operator  // Returns (1) plus (2).                    (Real, Real) => Real
case object Sub       extends Operator  // Returns (1) minus (2).                   (Real, Real) => Real
case object Mul       extends Operator  // Returns (1) times (2).                   (Real, Real) => Real
case object Div       extends Operator  // Returns (1) divided by (2).              (Real, Real) => Real
case object Mod       extends Operator  // Returns (1) mod (2).                     (Real, Real) => Real
case object Pow       extends Operator  // Returns (1) raised to the (2).           (Real, Real) => Real
case object Log       extends Operator  // Returns the natural logarithm of (1).    (Real) => Real
case object Sin       extends Operator  // Returns the sine of (1) in radians.      (Real) => Real
case object Cos       extends Operator  // Returns the cosine of (1) in radians.    (Real) => Real
case object Floor     extends Operator  // Returns the floor of (1).                (Real) => Real
case object Both      extends Operator  // Returns (1) && (2).                      (Flag, Flag) => Flag
case object Either    extends Operator  // Returns (1) || (2).                      (Flag, Flag) => Flag
case object Negate    extends Operator  // Returns the negation of (1).             (Flag) => Flag
case object Length    extends Operator  // Returns the string length of (1).        (Text) => Real
case object Slice     extends Operator  // Returns the substring of (1) in [2, 3).  (Text, Real, Real) => Text
case object Matches   extends Operator  // Returns whether (1) matches regex (2).   (Text, Text) => Flag
case object Contains  extends Operator  // Returns whether (2) is contained in (1). (Text, Text) => Flag
case object IndexOf   extends Operator  // Returns the index of (2) in (1).         (Text, Text) => Real
case object Equal     extends Operator  // Returns whether (1) equals (2).          (Any, Any) => Flag
case object Less      extends Operator  // Returns whether (1) is less than (2).    (Any, Any) => Flag
