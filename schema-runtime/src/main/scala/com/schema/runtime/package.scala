package com.schema

package object runtime {

  // We may assume without loss of generality that all keys and values are strings, because all
  // digital information must be representable as a string of ones and zeroes. Each key and value is
  // associated with a revision number, which the database uses to detect transaction conflicts.
  type Key = String
  type Revision = Long
  type Value = String

  // Implicit conversions.
  implicit def str2txn(value: String): Transaction = literal(value)
  implicit def bool2txn(value: Boolean): Transaction = literal(value)
  implicit def num2txn[T](value: T)(implicit num: Numeric[T]): Transaction = literal(value)

  // Core Operations.
  def read(k: Transaction): Transaction = Operation(Read, List(k))
  def write(k: Transaction, v: Transaction): Transaction = Operation(Write, List(k, v))
  def branch(c: Transaction, p: Transaction, f: Transaction): Transaction = Operation(Branch, List(c, p, f))
  def cons(x: Transaction, y: Transaction): Transaction = Operation(Cons, List(x, y))
  def repeat(t: Transaction, s: Transaction): Transaction = Operation(Repeat, List(t, s))
  def loop(f: Transaction, u: Transaction, s: Transaction, i: Transaction, b: Transaction): Transaction = Operation(Loop, List(f, u, s, i, b))
  def load(k: Transaction): Transaction = Operation(Load, List(k))
  def store(k: Transaction, v: Transaction): Transaction = Operation(Store, List(k, v))
  def literal(x: Boolean): Transaction = if (x) Literal.True else Literal.False
  def literal(x: String): Transaction = Literal(x)
  def literal[T](x: T)(implicit num: Numeric[T]): Transaction = Literal(num.toDouble(x).toString)
  def rollback(result: Transaction): Transaction = Operation(Rollback, List.empty)

  // Math Operations.
  lazy val E  : Transaction = Literal(math.E.toString)
  lazy val Pi : Transaction = Literal(math.Pi.toString)

  def add(x: Transaction, y: Transaction): Transaction = Operation(Add, List(x, y))
  def sub(x: Transaction, y: Transaction): Transaction = Operation(Sub, List(x, y))
  def mul(x: Transaction, y: Transaction): Transaction = Operation(Mul, List(x, y))
  def div(x: Transaction, y: Transaction): Transaction = Operation(Div, List(x, y))
  def mod(x: Transaction, y: Transaction): Transaction = Operation(Mod, List(x, y))
  def abs(x: Transaction): Transaction = branch(less(x, Literal.Zero), sub(Literal.Zero, x), x)

  def pow(x: Transaction, y: Transaction): Transaction = Operation(Pow, List(x, y))
  def exp(x: Transaction): Transaction = pow(E, x)
  def log(x: Transaction): Transaction = Operation(Log, List(x))
  def sqrt(x: Transaction): Transaction = pow(x, Literal.Half)

  def sin(x: Transaction): Transaction = Operation(Sin, List(x))
  def cos(x: Transaction): Transaction = Operation(Cos, List(x))
  def tan(x: Transaction): Transaction = div(sin(x), cos(x))
  def cot(x: Transaction): Transaction = div(cos(x), sin(x))
  def sec(x: Transaction): Transaction = div(Literal.One, cos(x))
  def csc(x: Transaction): Transaction = div(Literal.One, sin(x))
  def sinh(x: Transaction): Transaction = div(sub(exp(x), exp(sub(Literal.Zero, x))), Literal.Two)
  def cosh(x: Transaction): Transaction = div(add(exp(x), exp(sub(Literal.Zero, x))), Literal.Two)
  def tanh(x: Transaction): Transaction = div(sinh(x), cosh(x))
  def coth(x: Transaction): Transaction = div(cosh(x), sinh(x))
  def sech(x: Transaction): Transaction = div(Literal.One, cosh(x))
  def csch(x: Transaction): Transaction = div(Literal.One, sinh(x))

  def ceil(x: Transaction): Transaction = branch(equal(x, floor(x)), x, floor(x) + 1)
  def floor(x: Transaction): Transaction = Operation(Floor, List(x))
  def round(x: Transaction): Transaction = branch(less(sub(x, floor(x)), Literal.Half), floor(x), ceil(x))

  // String Operations.
  def length(x: Transaction): Transaction = Operation(Length, List(x))
  def concat(x: Transaction, y: Transaction): Transaction = Operation(Concat, List(x, y))
  def slice(x: Transaction, l: Transaction, h: Transaction): Transaction = Operation(Slice, List(x, l, h))

  // Logical Operations.
  def and(x: Transaction, y: Transaction): Transaction = Operation(And, List(x, y))
  def or(x: Transaction, y: Transaction): Transaction = Operation(Or, List(x, y))
  def not(x: Transaction): Transaction = Operation(Not, List(x))
  def equal(x: Transaction, y: Transaction): Transaction = Operation(Equal, List(x, y))
  def less(x: Transaction, y: Transaction): Transaction = Operation(Less, List(x, y))
  def matches(x: Transaction, y: Transaction): Transaction = Operation(Matches, List(x, y))
  def contains(x: Transaction, y: Transaction): Transaction = Operation(Contains, List(x, y))

  /**
   *
   * @param x
   */
  implicit class InfixTransaction(x: Transaction) {

    def unary_- : Transaction = sub(Literal.Zero, x)
    def unary_! : Transaction = not(x)
    def unary_~ : Transaction = not(x)

    def +(y: Transaction): Transaction = add(x, y)
    def -(y: Transaction): Transaction = sub(x, y)
    def *(y: Transaction): Transaction = mul(x, y)
    def /(y: Transaction): Transaction = div(x, y)
    def %(y: Transaction): Transaction = mod(x, y)

    def <(y: Transaction): Transaction = less(x, y)
    def >(y: Transaction): Transaction = not(or(equal(x, y), less(x, y)))
    def ==(y: Transaction): Transaction = equal(x, y)
    def !=(y: Transaction): Transaction = not(equal(x, y))
    def <=(y: Transaction): Transaction = or(equal(x, y), less(x, y))
    def >=(y: Transaction): Transaction = not(less(x, y))
    def &&(y: Transaction): Transaction = and(x, y)
    def ||(y: Transaction): Transaction = or(x, y)
    def max(y: Transaction): Transaction = branch(less(x, y), y, x)
    def min(y: Transaction): Transaction = branch(less(x, y), x, y)

    def ++(y: Transaction): Transaction = concat(x, y)
    def charAt(i: Transaction): Transaction = slice(x, i, add(i, Literal.One))
    def contains(y: Transaction): Transaction = runtime.contains(x, y)
    def endsWith(y: Transaction): Transaction = equal(x.substring(length(x) - length(y)), y)
    def startsWith(y: Transaction): Transaction = equal(x.substring(0, length(y)), y)
    def matches(y: Transaction): Transaction = runtime.matches(x, y)
    def substring(l: Transaction): Transaction = x.substring(l, length(x))
    def substring(l: Transaction, h: Transaction): Transaction = slice(x, l.min(h).max(0), h.max(l).min(length(x)))

  }

}
