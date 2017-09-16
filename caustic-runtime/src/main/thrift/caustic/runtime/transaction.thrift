namespace * caustic.runtime.thrift

/**
 * A database transaction. Transactions are composed of Literals and Expressions, which may be
 * combined to form complex abstract-syntax-trees and may be executed on any Database.
 */
union Transaction {
  1: Literal literal,
  2: Expression expression,
}

/**
 * An explicit value. Literals may be one of three primitive types: flag, real, or text. All other
 * types may be formed from compositions as them. Literals form the "leaves" of a Transaction.
 */
union Literal {
  1: bool flag,
  2: double real,
  3: string text,
}

/**
 * A value transformation. Expressions apply a transformation to Literals or the results of other
 * Expressions. Expressions form the "nodes" of a Transaction.
 */
union Expression {
  1: Read read,
  2: Write write,
  3: Load load,
  4: Store store,
  5: Cons cons,
  6: Prefetch prefetch,
  7: Repeat repeat,
  8: Branch branch,
  9: Rollback rollback,
  10: Add add,
  11: Sub sub,
  12: Mul mul,
  13: Div div,
  14: Mod mod,
  15: Pow pow,
  16: Log log,
  17: Sin sin,
  18: Cos cos,
  19: Floor floor,
  20: Both both,
  21: Either either,
  22: Negate negate,
  23: Length length,
  24: Slice slice,
  25: Matches matches,
  26: Contains contains,
  27: IndexOf indexOf,
  28: Equal equal,
  29: Less less,
}

/**
 * Returns the value of the database key.
 *
 * @param key Text.
 */
struct Read {
  1: required Transaction key,
}

/**
 * Updates the value of the database key.
 *
 * @param key Text.
 * @param value Any.
 */
struct Write {
  1: required Transaction key,
  2: required Transaction value,
}

/**
 * Returns the value of the local variable.
 *
 * @param variable Text.
 */
struct Load {
  1: required Transaction variable,
}

/**
 * Updates the value of the local variable.
 *
 * @param variable Text.
 * @param value Any.
 */
struct Store {
  1: required Transaction variable,
  2: required Transaction value,
}

/**
 * Performs first and then second.
 *
 * @param first Any.
 * @param second Any.
 */
struct Cons {
  1: required Transaction first,
  2: required Transaction second,
}

/**
 * Prefetches the array-delimited list of keys.
 *
 * @param keys Text.
 */
struct Prefetch {
  1: required Transaction keys,
}

/**
 * Performs the body until the condition is not satisfied.
 *
 * @param condition Flag.
 * @param body Any.
 */
struct Repeat {
  1: required Transaction condition,
  2: required Transaction body,
}

/**
 * Performs success if the condition is satisfied, or failure otherwise.
 *
 * @param condition Flag.
 * @param success Any.
 * @param failure Any.
 */
struct Branch {
  1: required Transaction condition,
  2: required Transaction success,
  3: required Transaction failure,
}

/**
 * Discards any changes performed by the transaction, and terminates execution.
 *
 * @param message Text.
 */
struct Rollback {
  1: required Transaction message,
}

/**
 * Returns x plus y.
 *
 * @param x Real.
 * @param y Real.
 */
struct Add {
  1: required Transaction x,
  2: required Transaction y,
}

/**
 * Returns x minus y.
 *
 * @param x Real.
 * @param y Real.
 */
struct Sub {
  1: required Transaction x,
  2: required Transaction y,
}

/**
 * Returns x times y.
 *
 * @param x Real.
 * @param y Real.
 */
struct Mul {
  1: required Transaction x,
  2: required Transaction y,
}

/**
 * Returns x divided by y.
 *
 * @param x Real.
 * @param y Real.
 */
struct Div {
  1: required Transaction x,
  2: required Transaction y,
}

/**
 * Returns x modulo y.
 *
 * @param x Real.
 * @param y Real.
 */
struct Mod {
  1: required Transaction x,
  2: required Transaction y,
}

/**
 * Returns x to the power of y.
 *
 * @param x Real.
 * @param y Real.
 */
struct Pow {
  1: required Transaction x,
  2: required Transaction y,
}

/**
 * Returns the natural log of x.
 *
 * @param x Real.
 */
struct Log {
  1: required Transaction x,
}

/**
 * Returns the sine of x.
 *
 * @param x Real.
 */
struct Sin {
  1: required Transaction x,
}

/**
 * Returns the cosine of x.
 *
 * @param x Real.
 */
struct Cos {
  1: required Transaction x,
}

/**
 * Returns the floor of x.
 *
 * @param x Real.
 */
struct Floor {
  1: required Transaction x,
}

/**
 * Returns a flag containing the bitwise and of x and y.
 *
 * @param x Flag.
 * @param y Flag.
 */
struct Both {
  1: required Transaction x,
  2: required Transaction y,
}

/**
 * Returns a flag containing the bitwise or of x and y.
 *
 * @param x Flag.
 * @param y Flag.
 */
struct Either {
  1: required Transaction x,
  2: required Transaction y,
}

/**
 * Returns a flag containing bitwise negation of x.
 *
 * @param x Flag.
 */
struct Negate {
  1: required Transaction x,
}

/**
 * Returns the length of x.
 *
 * @param x Text.
 */
struct Length {
  1: required Transaction x,
}

/**
 * Returns the substring of x within [lower, higher).
 *
 * @param x Text.
 * @param lower Real.
 * @param higher Real.
 */
struct Slice {
  1: required Transaction x,
  2: required Transaction lower,
  3: required Transaction higher,
}

/**
 * Returns whether or not x matches the specified regular expression.
 *
 * @param x Text.
 * @param regex Text.
 */
struct Matches {
  1: required Transaction x,
  2: required Transaction regex,
}

/**
 * Returns a flag indicating whether or not y is contained in x.
 *
 * @param x Text.
 * @param y Text.
 */
struct Contains {
  1: required Transaction x,
  2: required Transaction y,
}

/**
 * Returns the index of y in x.
 *
 * @param x Text.
 * @param y Text.
 */
struct IndexOf {
  1: required Transaction x,
  2: required Transaction y,
}

/**
 * Returns a flag indicating whether or not x and y have the same type and value.
 *
 * @param x Any.
 * @param y Any.
 */
struct Equal {
  1: required Transaction x,
  2: required Transaction y,
}

/**
 * Returns a flag indicating whether or not x is strictly less than y.
 *
 * @param x Any.
 * @param y Any.
 */
struct Less {
  1: required Transaction x,
  2: required Transaction y,
}