package com.schema

package object runtime {

  // We may assume without loss of generality that all keys and values are strings, because all
  // digital information must be representable as a string of ones and zeroes. Each key and value is
  // associated with a revision number, which the database uses to detect transaction conflicts.
  type Key = String
  type Revision = Long
  type Value = String

  // Implicit conversions to literals.
  implicit def str2lit(value: String): Literal = literal(value)
  implicit def bol2lit(value: Boolean): Literal = literal(value)
  implicit def num2lit[T](value: T)(implicit num: Numeric[T]): Literal = literal(value)

  // Remote Key-Value Pairs.
  def read(k: Transaction): Transaction = Operation(Read, List(k))
  def write(k: Transaction, v: Transaction): Transaction = Operation(Write, List(k, v))

  // Local Variables.
  def load(k: Transaction): Transaction = Operation(Load, List(k))
  def store(k: Transaction, v: Transaction): Transaction = Operation(Store, List(k, v))

  // Literals.
  def literal(x: Boolean): Literal = if (x) Literal.True else Literal.False
  def literal(x: String): Literal = Literal(x)
  def literal[T](x: T)(implicit num: Numeric[T]): Literal = Literal(num.toDouble(x).toString)

  // Control Flow Operations.
  def rollback(result: Transaction): Transaction =
    Operation(Rollback, List.empty)

  def cons(a: Transaction, b: Transaction): Transaction = (a, b) match {
    case (_: Literal, y) => y
    case _ => Operation(Cons, List(a, b))
  }

  def branch(a: Transaction, b: Transaction, c: Transaction): Transaction = (a, b, c) match {
    case (x: Literal, y, z) => if (x != Literal.False && x != Literal.Empty) y else z
    case _ => Operation(Branch, List(a, b, c))
  }

  def prefetch(a: Transaction): Transaction = a match {
    case l: Literal => l.value.split(",")
      .map(k => read(k))
      .reduceLeftOption((a, b) => cons(a, b))
      .getOrElse(Literal.Empty)
    case _ => Operation(Prefetch, List(a))
  }

  def repeat(a: Transaction, b: Transaction): Transaction = (a, b) match {
    case (x: Literal, y) =>
      require(x != Literal.False && x != Literal.Empty, "Infinite loop detected.")
      Literal.Empty
    case _ => Operation(Repeat, List(a, b))
  }

  // Math Operations.
  def add(a: Transaction, b: Transaction): Transaction = (a, b) match {
    case (Literal(x), Literal(y)) => x.toDouble + y.toDouble
    case _ => Operation(Add, List(a, b))
  }

  def sub(a: Transaction, b: Transaction): Transaction = (a, b) match {
    case (Literal(x), Literal(y)) => x.toDouble - y.toDouble
    case _ => Operation(Sub, List(a, b))
  }

  def mul(a: Transaction, b: Transaction): Transaction = (a, b) match {
    case (Literal(x), Literal(y)) => x.toDouble * y.toDouble
    case _ => Operation(Mul, List(a, b))
  }

  def div(a: Transaction, b: Transaction): Transaction = (a, b) match {
    case (Literal(x), Literal(y)) => x.toDouble / y.toDouble
    case _ => Operation(Div, List(a, b))
  }

  def mod(a: Transaction, b: Transaction): Transaction = (a, b) match {
    case (Literal(x), Literal(y)) => x.toDouble % y.toDouble
    case _ => Operation(Mod, List(a, b))
  }

  def pow(a: Transaction, b: Transaction): Transaction = (a, b) match {
    case (Literal(x), Literal(y)) => math.pow(x.toDouble, y.toDouble)
    case _ => Operation(Pow, List(a, b))
  }

  def log(a: Transaction): Transaction = a match {
    case Literal(x) => math.log(x.toDouble)
    case _ => Operation(Log, List(a))
  }

  def sin(a: Transaction): Transaction = a match {
    case Literal(x) => math.sin(x.toDouble)
    case _ => Operation(Sin, List(a))
  }

  def cos(a: Transaction): Transaction = a match {
    case Literal(x) => math.cos(x.toDouble)
    case _ => Operation(Cos, List(a))
  }

  def floor(a: Transaction): Transaction = a match {
    case Literal(x) => math.floor(x.toDouble)
    case _ => Operation(Floor, List(a))
  }

  // String Operations.
  def length(a: Transaction): Transaction = a match {
    case Literal(x) => x.length
    case _ => Operation(Length, List(a))
  }

  def concat(a: Transaction, b: Transaction): Transaction = (a, b) match {
    case (Literal(x), Literal(y)) => x + y
    case _ => Operation(Concat, List(a, b))
  }

  def slice(a: Transaction, b: Transaction, c: Transaction): Transaction = (a, b, c) match {
    case (Literal(x), Literal(y), Literal(z)) => x.substring(y.toDouble.toInt, z.toDouble.toInt)
    case _ => Operation(Slice, List(a, b, c))
  }

  def matches(a: Transaction, b: Transaction): Transaction = (a, b) match {
    case (Literal(x), Literal(y)) => if (x.matches(y)) Literal.True else Literal.False
    case _ => Operation(Matches, List(a, b))
  }

  def contains(a: Transaction, b: Transaction): Transaction = (a, b) match {
    case (Literal(x), Literal(y)) => if (x.contains(y)) Literal.True else Literal.False
    case _ => Operation(Contains, List(a, b))
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
