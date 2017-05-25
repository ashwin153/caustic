package schema

import runtime.Operation._

package object runtime {

  // We may assume without loss of generality that all keys and values are strings, because all
  // digital information must be representable as a string of ones and zeroes. Each key and value is
  // associated with a revision number, which the database uses to detect transaction conflicts.
  type Key = String
  type Revision = Long
  type Value = String

  // Remote Key-Value Pairs.
  def read(key: Transaction): Transaction = Operation(Read, List(key))
  def write(key: Transaction, value: Transaction): Transaction = Operation(Write, List(key, value))

  // Literals.
  def literal(x: Boolean): Literal = if (x) Literal.True else Literal.False
  def literal(x: String): Literal = Literal(x)
  def literal[T](x: T)(implicit num: Numeric[T]): Literal = Literal(num.toDouble(x).toString)

  // Local Variables.
  def load(name: Transaction): Transaction = Operation(Load, List(name))
  def store(name: Transaction, value: Transaction): Transaction = Operation(Store, List(name, value))

  // Traps.
  def rollback(message: Transaction): Transaction = Operation(Rollback, List(message))

  // Control Flow Operations.
  def cons(first: Transaction, second: Transaction): Transaction = (first, second) match {
    case (_: Literal, y) => y
    case _ => Operation(Cons, List(first, second))
  }

  def branch(cond: Transaction, pass: Transaction, fail: Transaction): Transaction = (cond, pass, fail) match {
    case (x: Literal, y, z) => if (x != Literal.False) y else z
    case _ => Operation(Branch, List(cond, pass, fail))
  }

  def prefetch(keys: Transaction): Transaction = keys match {
    case l: Literal => l.value.split(",")
      .map(k => read(literal(k)))
      .reduceLeftOption((a, b) => cons(a, b))
      .getOrElse(Literal.Empty)
    case _ => Operation(Prefetch, List(keys))
  }

  def repeat(cond: Transaction, body: Transaction): Transaction = (cond, body) match {
    case (x: Literal, _) =>
      require(x != Literal.False && x != Literal.Empty, "Infinite loop detected.")
      Literal.Empty
    case _ => Operation(Repeat, List(cond, body))
  }

  // Math Operations.
  def add(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (Literal(a), Literal(b)) => literal(a.toDouble + b.toDouble)
    case _ => Operation(Add, List(x, y))
  }

  def sub(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (Literal(a), Literal(b)) => literal(a.toDouble - b.toDouble)
    case _ => Operation(Sub, List(x, y))
  }

  def mul(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (Literal(a), Literal(b)) => literal(a.toDouble * b.toDouble)
    case _ => Operation(Mul, List(x, y))
  }

  def div(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (Literal(a), Literal(b)) => literal(a.toDouble / b.toDouble)
    case _ => Operation(Div, List(x, y))
  }

  def mod(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (Literal(a), Literal(b)) => literal(a.toDouble % b.toDouble)
    case _ => Operation(Mod, List(x, y))
  }

  def pow(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (Literal(a), Literal(b)) => literal(math.pow(a.toDouble, b.toDouble))
    case _ => Operation(Pow, List(x, y))
  }

  def log(x: Transaction): Transaction = x match {
    case Literal(a) => literal(math.log(a.toDouble))
    case _ => Operation(Log, List(x))
  }

  def sin(x: Transaction): Transaction = x match {
    case Literal(a) => literal(math.sin(a.toDouble))
    case _ => Operation(Sin, List(x))
  }

  def cos(x: Transaction): Transaction = x match {
    case Literal(a) => literal(math.cos(a.toDouble))
    case _ => Operation(Cos, List(x))
  }

  def floor(x: Transaction): Transaction = x match {
    case Literal(a) => literal(math.floor(a.toDouble))
    case _ => Operation(Floor, List(x))
  }

  // String Operations.
  def length(str: Transaction): Transaction = str match {
    case Literal(x) => literal(x.length)
    case _ => Operation(Length, List(str))
  }

  def concat(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (Literal(a), Literal(b)) => literal(a + b)
    case _ => Operation(Concat, List(x, y))
  }

  def slice(str: Transaction, low: Transaction, high: Transaction): Transaction = (str, low, high) match {
    case (Literal(x), Literal(y), Literal(z)) => literal(x.substring(y.toDouble.toInt, z.toDouble.toInt))
    case _ => Operation(Slice, List(str, low, high))
  }

  def matches(str: Transaction, regex: Transaction): Transaction = (str, regex) match {
    case (Literal(x), Literal(y)) => if (x.matches(y)) Literal.True else Literal.False
    case _ => Operation(Matches, List(str, regex))
  }

  def contains(str: Transaction, sub: Transaction): Transaction = (str, sub) match {
    case (Literal(x), Literal(y)) => if (x.contains(y)) Literal.True else Literal.False
    case _ => Operation(Contains, List(str, sub))
  }

  // Logical Operations.
  def and(a: Transaction, b: Transaction): Transaction = (a, b) match {
    case (x: Literal, y: Literal) => if (x != Literal.False && y != Literal.False) Literal.True else Literal.False
    case _ => Operation(And, List(a, b))
  }

  def or(a: Transaction, b: Transaction): Transaction = (a, b) match {
    case (x: Literal, y: Literal) => if (x != Literal.False || y != Literal.False) Literal.True else Literal.False
    case _ => Operation(Or, List(a, b))
  }

  def not(a: Transaction): Transaction = a match {
    case x: Literal => if (x != Literal.False) Literal.False else Literal.True
    case _ => Operation(Not, List(a))
  }

  def equal(a: Transaction, b: Transaction): Transaction = (a, b) match {
    case (Literal(x), Literal(y)) => if (x == y) Literal.True else Literal.False
    case _ => Operation(Equal, List(a, b))
  }

  def less(a: Transaction, b: Transaction): Transaction = (a, b) match {
    case (Literal(x), Literal(y)) => if (x < y) Literal.True else Literal.False
    case _ => Operation(Less, List(a, b))
  }

}