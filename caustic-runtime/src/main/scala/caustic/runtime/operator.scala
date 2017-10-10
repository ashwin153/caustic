package caustic.runtime

/**
 * An enumeration of the various operators supported by the library. Ensure that this enumeration
 * remains consistent with transaction.thrift, and any new operators must also be added to the
 * parser package object and to the TransactionalDatabase execution engine.
 */
sealed trait Operator
case object Read extends Operator
case object Write extends Operator
case object Load extends Operator
case object Store extends Operator
case object Cons extends Operator
case object Repeat extends Operator
case object Branch extends Operator
case object Rollback extends Operator
case object Prefetch extends Operator
case object Add extends Operator
case object Sub extends Operator
case object Mul extends Operator
case object Div extends Operator
case object Mod extends Operator
case object Pow extends Operator
case object Log extends Operator
case object Sin extends Operator
case object Cos extends Operator
case object Floor extends Operator
case object Both extends Operator
case object Either extends Operator
case object Negate extends Operator
case object Length extends Operator
case object Slice extends Operator
case object Matches extends Operator
case object Contains extends Operator
case object IndexOf extends Operator
case object Equal extends Operator
case object Less extends Operator
