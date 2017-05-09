package com.schema.runtime

sealed trait Operator
case object Read     extends Operator  // Lookup the version and value of a remote key.
case object Write    extends Operator  // Update the version and value of a remote key.
case object Load     extends Operator  // Lookup the value of a local key.
case object Store    extends Operator  // Update the value of a local key.
case object Cons     extends Operator  // Sequentially evaluate arguments.
case object Loop     extends Operator  // Repeat while the condition is not satisfied.
case object Repeat   extends Operator  // Repeat the specified number of times.
case object Branch   extends Operator  // Jump to third if first is empty, and second otherwise.
case object Rollback extends Operator  // Rolls back the transaction.
case object Add      extends Operator  // Sum of the two arguments.
case object Sub      extends Operator  // Difference of the two arguments.
case object Mul      extends Operator  // Product of the two arguments.
case object Div      extends Operator  // Quotient of the two arguments.
case object Mod      extends Operator  // Modulo of the two arguments.
case object Pow      extends Operator  // Power of the first argument to the second.
case object Log      extends Operator  // Natural logarithm of the argument.
case object Sin      extends Operator  // Sine of the argument.
case object Cos      extends Operator  // Cosine of the argument.
case object Floor    extends Operator  // Largest integer less than the argument.
case object And      extends Operator  // Checks that both arguments are non-empty.
case object Not      extends Operator  // Opposite of the argument.
case object Or       extends Operator  // Checks that either argument is non-empty.
case object Length   extends Operator  // Number of characters in the argument.
case object Slice    extends Operator  // Substring of the first argument bounded by  others.
case object Concat   extends Operator  // Concatenation of the two arguments.
case object Matches  extends Operator  // Regular expression of second argument matches first.
case object Contains extends Operator  // Checks that the first argument contains the second.
case object Equal    extends Operator  // Checks that the two arguments are equal.
case object Less     extends Operator  // Checks that the first argument is less than the other.