package caustic.syntax

import caustic.runtime.thrift

/**
 * A static library for generating Thrift transactions. Used by the generated sources to dynamically
 * build and execute transactions on the underlying database. Must be imported into all source files
 * in order for them to compile.
 */
object Codec {

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
  def read(k: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.read(new thrift.Read(k)))
  def write(k: thrift.Transaction, v: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.write(new thrift.Write(k, v)))
  def load(n: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.load(new thrift.Load(n)))
  def store(n: thrift.Transaction, v: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.store(new thrift.Store(n, v)))
  def branch(c: thrift.Transaction, p: thrift.Transaction, f: thrift.Transaction = Empty): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.branch(new thrift.Branch(c, p, f)))
  def cons(a: thrift.Transaction, rest: thrift.Transaction*): thrift.Transaction =
    rest.foldLeft(a)((a, b) => thrift.Transaction.expression(thrift.Expression.cons(new thrift.Cons(a, b))))
  def repeat(c: thrift.Transaction, b: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.repeat(new thrift.Repeat(c, b)))
  def prefetch(k: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.prefetch(new thrift.Prefetch(k)))
  def rollback(r: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.rollback(new thrift.Rollback(r)))

  // Math Expressions.
  def add(x: thrift.Transaction, rest: thrift.Transaction*): thrift.Transaction =
    rest.foldLeft(x)((a, b) => thrift.Transaction.expression(thrift.Expression.add(new thrift.Add(a, b))))
  def sub(x: thrift.Transaction, rest: thrift.Transaction*): thrift.Transaction =
    rest.foldLeft(x)((a, b) => thrift.Transaction.expression(thrift.Expression.sub(new thrift.Sub(a, b))))
  def mul(x: thrift.Transaction, rest: thrift.Transaction*): thrift.Transaction =
    rest.foldLeft(x)((a, b) => thrift.Transaction.expression(thrift.Expression.mul(new thrift.Mul(a, b))))
  def div(x: thrift.Transaction, rest: thrift.Transaction*): thrift.Transaction =
    rest.foldLeft(x)((a, b) => thrift.Transaction.expression(thrift.Expression.div(new thrift.Div(a, b))))
  def mod(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.mod(new thrift.Mod(x, y)))
  def pow(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.pow(new thrift.Pow(x, y)))
  def log(x: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.log(new thrift.Log(x)))
  def sin(x: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.sin(new thrift.Sin(x)))
  def cos(x: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.cos(new thrift.Cos(x)))
  def floor(x: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.floor(new thrift.Floor(x)))

  def abs(x: thrift.Transaction): thrift.Transaction =
    branch(lt(x, Zero), sub(Zero, x), x)
  def exp(x: thrift.Transaction): thrift.Transaction =
    pow(E, x)
  def tan(x: thrift.Transaction): thrift.Transaction =
    div(sin(x), cos(x))
  def cot(x: thrift.Transaction): thrift.Transaction =
    div(cos(x), sin(x))
  def sec(x: thrift.Transaction): thrift.Transaction =
    div(One, cos(x))
  def csc(x: thrift.Transaction): thrift.Transaction =
    div(One, sin(x))
  def sinh(x: thrift.Transaction): thrift.Transaction =
    div(sub(exp(x), exp(sub(Zero, x))), Two)
  def cosh(x: thrift.Transaction): thrift.Transaction =
    div(add(exp(x), exp(sub(Zero, x))), Two)
  def tanh(x: thrift.Transaction): thrift.Transaction =
    div(sinh(x), cosh(x))
  def coth(x: thrift.Transaction): thrift.Transaction =
    div(cosh(x), sinh(x))
  def sech(x: thrift.Transaction): thrift.Transaction =
    div(One, cosh(x))
  def csch(x: thrift.Transaction): thrift.Transaction =
    div(One, sinh(x))
  def sqrt(x: thrift.Transaction): thrift.Transaction =
    pow(x, Half)
  def ceil(x: thrift.Transaction): thrift.Transaction =
    branch(eq(x, floor(x)), x, add(floor(x), One))
  def round(x: thrift.Transaction): thrift.Transaction =
    branch(lt(sub(x, floor(x)), Half), floor(x), ceil(x))

  // String Expressions.
  def contains(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.contains(new thrift.Contains(x, y)))
  def length(x: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.length(new thrift.Length(x)))
  def slice(x: thrift.Transaction, l: thrift.Transaction, h: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.slice(new thrift.Slice(x, l, h)))
  def slice(x: thrift.Transaction, l: thrift.Transaction): thrift.Transaction =
    slice(x, l, length(x))
  def matches(x: thrift.Transaction, r: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.matches(new thrift.Matches(x, r)))
  def indexOf(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.indexOf(new thrift.IndexOf(x, y)))

  // Logical Expressions.
  def and(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.both(new thrift.Both(x, y)))
  def or(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.either(new thrift.Either(x, y)))
  def not(x: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.negate(new thrift.Negate(x)))
  def eq(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.equal(new thrift.Equal(x, y)))
  def ne(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    not(eq(x, y))
  def le(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    or(eq(x, y), lt(x, y))
  def lt(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    thrift.Transaction.expression(thrift.Expression.less(new thrift.Less(x, y)))
  def ge(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    not(lt(x, y))
  def gt(x: thrift.Transaction, y: thrift.Transaction): thrift.Transaction =
    not(le(x, y))

}
