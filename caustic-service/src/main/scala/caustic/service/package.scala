package caustic

import caustic.runtime.thrift

import scala.language.implicitConversions

package object service {

  // Implicit Conversions.
  implicit def bol2flag(x: Boolean): thrift.Transaction =
    flag(x)
  implicit def num2real[T](x: T)(implicit num: Numeric[T]): thrift.Transaction =
    real(num.toDouble(x))
  implicit def str2text(x: String): thrift.Transaction =
    text(x)

  // Constants.
  val True: thrift.Transaction = flag(true)
  val False: thrift.Transaction = flag(false)
  val Zero: thrift.Transaction = real(0)
  val One: thrift.Transaction = real(1)
  val Two: thrift.Transaction = real(2)
  val Half: thrift.Transaction = real(0.5)
  val E: thrift.Transaction = real(math.E)
  val Pi: thrift.Transaction = real(math.Pi)
  val Empty: thrift.Transaction = text("")

  // Literals Values.
  def flag(x: Boolean): thrift.Transaction =
    thrift.Transaction.literal(thrift.Literal.flag(x))
  def real[T](x: T)(implicit num: Numeric[T]): thrift.Transaction =
    thrift.Transaction.literal(thrift.Literal.real(num.toDouble(x)))
  def text(x: String): thrift.Transaction =
    thrift.Transaction.literal(thrift.Literal.text(x))

  // Basic Expressions.
  def branch(c: thrift.Transaction, p: thrift.Transaction, f: thrift.Transaction = Empty): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.branch(new thrift.Branch(c, p, f)))
  def cons(a: thrift.Transaction, b: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.cons(new thrift.Cons(a, b)))
  def load(n: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.load(new thrift.Load(n)))
  def prefetch(k: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.prefetch(new thrift.Prefetch(k)))
  def read(k: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.read(new thrift.Read(k)))
  def repeat(c: thrift.Transaction, b: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.repeat(new thrift.Repeat(c, b)))
  def rollback(r: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.rollback(new thrift.Rollback(r)))
  def store(n: thrift.Transaction, v: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.store(new thrift.Store(n, v)))
  def write(k: thrift.Transaction, v: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.write(new thrift.Write(k, v)))

  // Math Expressions.
  def abs(x: thrift.Transaction): thrift.Transaction =
    branch(lessThan(x, Zero), sub(Zero, x), x)
  def add(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.add(new thrift.Add(x, y)))
  def ceil(x: thrift.Transaction): thrift.Transaction =
    branch(equal(x, floor(x)), x, add(floor(x), One))
  def cos(x: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.cos(new thrift.Cos(x)))
  def cosh(x: thrift.Transaction): thrift.Transaction =
    div(add(exp(x), exp(sub(Zero, x))), Two)
  def cot(x: thrift.Transaction): thrift.Transaction =
    div(cos(x), sin(x))
  def coth(x: thrift.Transaction): thrift.Transaction =
    div(cosh(x), sinh(x))
  def csc(x: thrift.Transaction): thrift.Transaction =
    div(One, sin(x))
  def csch(x: thrift.Transaction): thrift.Transaction =
    div(One, sinh(x))
  def div(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.div(new thrift.Div(x, y)))
  def exp(x: thrift.Transaction): thrift.Transaction =
    pow(E, x)
  def floor(x: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.floor(new thrift.Floor(x)))
  def log(x: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.log(new thrift.Log(x)))
  def mod(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.mod(new thrift.Mod(x, y)))
  def mul(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.mul(new thrift.Mul(x, y)))
  def pow(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.pow(new thrift.Pow(x, y)))
  def round(x: thrift.Transaction): thrift.Transaction =
    branch(lessThan(sub(x, floor(x)), Half), floor(x), ceil(x))
  def sec(x: thrift.Transaction): thrift.Transaction =
    div(One, cos(x))
  def sech(x: thrift.Transaction): thrift.Transaction =
    div(One, cosh(x))
  def sin(x: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.sin(new thrift.Sin(x)))
  def sinh(x: thrift.Transaction): thrift.Transaction =
    div(sub(exp(x), exp(sub(Zero, x))), Two)
  def sqrt(x: thrift.Transaction): thrift.Transaction =
    pow(x, Half)
  def sub(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.sub(new thrift.Sub(x, y)))
  def tan(x: thrift.Transaction): thrift.Transaction =
    div(sin(x), cos(x))
  def tanh(x: thrift.Transaction): thrift.Transaction =
    div(sinh(x), cosh(x))

  // String Expressions.
  def charAt(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    slice(x, y, add(y, One))
  def concat(x: thrift.Transaction*): thrift.Transaction =
    x.foldLeft(Empty)(add)
  def contains(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.contains(new thrift.Contains(x, y)))
  def indexOf(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.indexOf(new thrift.IndexOf(x, y)))
  def length(x: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.length(new thrift.Length(x)))
  def matches(x: thrift.Transaction, r: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.matches(new thrift.Matches(x, r)))
  def slice(x: thrift.Transaction, l: thrift.Transaction, h: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.slice(new thrift.Slice(x, l, h)))
  def slice(x: thrift.Transaction, l: thrift.Transaction): thrift.Transaction =
    slice(x, l, length(x))

  // Logical Expressions.
  def and(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.both(new thrift.Both(x, y)))
  def not(x: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.negate(new thrift.Negate(x)))
  def or(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.either(new thrift.Either(x, y)))

  def equal(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.equal(new thrift.Equal(x, y)))
  def greaterEqual(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    not(lessThan(x, y))
  def greaterThan(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    not(lessEqual(x, y))
  def lessEqual(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    or(equal(x, y), lessThan(x, y))
  def lessThan(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.less(new thrift.Less(x, y)))
  def notEqual(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    not(equal(x, y))

}
