namespace * caustic.runtime.thrift

/**
 * A database transaction. Transactions form an implicit abstract syntax tree, in which the nodes
 * are Expressions and the leaves are Literals. Transactions may be executed by a Database.
 */
union Transaction {
  1: Literal literal,
  2: Expression expression,
}

/**
 * A literal value. Literals are the only way to specify explicit values within a Transaction.
 * Therefore, Literals form the leaves of the abstract syntax tree in a Transaction.
 */
union Literal {
  1: bool flag,
  2: double real,
  3: string text,
}

/**
 * A transactional operation. Expressions apply Operators to operands that are either Literals or
 * the output of other Expressions. Expressions form the nodes of the abstract syntax tree in a
 * Transaction.
 */
struct Expression {
  1: required Operator operator,
  2: required list<Transaction> operands,
}

enum Operator {
  READ        = 0x100, // Lookup the latest revision of a remote key(s).
  WRITE       = 0x201, // Update the latest revision of a remote key.
  LOAD        = 0x102, // Lookup the value of a local key.
  STORE       = 0x203, // Update the value of a local key.
  CONS        = 0x204, // Sequentially evaluate arguments.
  PREFETCH    = 0x105, // Prefetches the comma delimited list of keys.
  REPEAT      = 0x206, // Repeat while the condition is not satisfied.
  BRANCH      = 0x307, // Jump to third if first is empty,and second otherwise.
  ROLLBACK    = 0x108, // Converts the transaction into a read-only transaction.
  ADD         = 0x209, // Sum of the two arguments.
  SUB         = 0x20A, // Difference of the two arguments.
  MUL         = 0x20B, // Product of the two arguments.
  DIV         = 0x20C, // Quotient of the two arguments.
  MOD         = 0x20D, // Modulo of the two arguments.
  POW         = 0x20E, // Power of the first argument to the second.
  LOG         = 0x10F, // Natural logarithm of the argument.
  SIN         = 0x110, // Sine of the argument.
  COS         = 0x111, // Cosine of the argument.
  FLOOR       = 0x112, // Largest integer less than the argument.
  AND         = 0x213, // Checks that both arguments are non-empty.
  NOT         = 0x114, // Opposite of the argument.
  OR          = 0x215, // Checks that either argument is non-empty.
  LENGTH      = 0x116, // Number of characters in the argument.
  SLICE       = 0x317, // Substring of the first argument bounded by  others.
  CONCAT      = 0x218, // Concatenation of the two arguments.
  MATCHES     = 0x219, // Regular expression of second argument matches first.
  CONTAINS    = 0x21A, // Checks that the first argument contains the second.
  EQUAL       = 0x21B, // Checks that the two arguments are equal.
  LESS        = 0x21C, // Checks that the first argument is less than the other.
}

