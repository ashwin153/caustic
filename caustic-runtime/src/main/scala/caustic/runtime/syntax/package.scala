package caustic.runtime

import syntax.Language._
import syntax.Context._
import scala.language.implicitConversions

package object syntax extends Language {

  // Internal Delimiters.
  val FieldDelimiter: String = "@"
  val ArrayDelimiter: String = ","
  val LocalDelimiter: String = "$"

  // Implicit Conversions.
  implicit def str2lit(value: String): Literal = literal(value)
  implicit def num2lit[T](value: T)(implicit num: Numeric[T]): Literal = literal(value)
  implicit def bol2lit(value: Boolean): Literal = literal(value)
  implicit def pxy2txn(proxy: Proxy): Transaction = read(proxy.key)
  implicit def var2txn(variable: Variable): Transaction = load(variable.name)
  implicit def pxy2ops(proxy: Proxy): TransactionOps = pxy2txn(proxy)
  implicit def var2ops(variable: Variable): TransactionOps = var2txn(variable)
  implicit def fld2obj(field: Field): Object = Object(read(field.key))
  implicit def rng2int(range: Range): Interval = Interval(range.start, range.end, range.step, range.isInclusive)

  // Additional Math Operations.
  lazy val E : Transaction = literal(math.E)
  lazy val Pi: Transaction = literal(math.Pi)

  def abs(x: Transaction): Transaction = branch(less(x, Literal.Zero), sub(Literal.Zero, x), x)
  def exp(x: Transaction): Transaction = pow(E, x)
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
  def sqrt(x: Transaction): Transaction = pow(x, Literal.Half)
  def ceil(x: Transaction): Transaction = branch(equal(x, floor(x)), x, floor(x) + Literal.One)
  def round(x: Transaction): Transaction = branch(less(sub(x, floor(x)), Literal.Half), floor(x), ceil(x))

  // Transaction Operations.
  implicit class TransactionOps(x: Transaction) {

    def unary_- : Transaction = sub(Literal.Zero, x)
    def unary_! : Transaction = not(x)

    def +(y: Transaction): Transaction = add(x, y)
    def -(y: Transaction): Transaction = sub(x, y)
    def *(y: Transaction): Transaction = mul(x, y)
    def /(y: Transaction): Transaction = div(x, y)
    def %(y: Transaction): Transaction = mod(x, y)

    def <(y: Transaction): Transaction = less(x, y)
    def >(y: Transaction): Transaction = not(or(equal(x, y), less(x, y)))
    def <>(y: Transaction): Transaction = not(equal(x, y))
    def <=(y: Transaction): Transaction = or(equal(x, y), less(x, y))
    def >=(y: Transaction): Transaction = not(less(x, y))
    def &&(y: Transaction): Transaction = and(x, y)
    def ||(y: Transaction): Transaction = or(x, y)
    def ===(y: Transaction): Transaction = equal(x, y)
    def max(y: Transaction): Transaction = branch(less(x, y), y, x)
    def min(y: Transaction): Transaction = branch(less(x, y), x, y)

    def to(y: Transaction): Interval = Interval(x, y, Literal.One, inclusive = true)
    def until(y: Transaction): Interval = Interval(x, y, Literal.One, inclusive = false)

    def ++(y: Transaction): Transaction = concat(x, y)
    def isEmpty: Transaction = x === Literal.Empty
    def nonEmpty: Transaction = x <> Literal.Empty
    def charAt(i: Transaction): Transaction = slice(x, i, add(i, Literal.One))
    def contains(y: Transaction): Transaction = schema.runtime.contains(x, y)
    def endsWith(y: Transaction): Transaction = equal(x.substring(length(x) - length(y)), y)
    def startsWith(y: Transaction): Transaction = equal(x.substring(0, length(y)), y)
    def matches(y: Transaction): Transaction = schema.runtime.matches(x, y)
    def substring(l: Transaction): Transaction = x.substring(l, length(x))
    def substring(l: Transaction, h: Transaction): Transaction = slice(x, l, h)

    def indexOf(y: Transaction, from: Transaction = 0): Transaction =
      block(
        store("$indexOf", -1),
        store("$i", from),
        repeat(
          load("$i") < (length(x) - length(y)) && load("$indexOf") < 0,
          branch(
            x.substring(load("$i"), load("$i") + length(y)) === y,
            store("$indexOf", load("$i")),
            store("$i", load("$i") + 1))),
        load("$indexOf")
      )
  }

  // Object Operations.
  implicit class ObjectOps(x: Object) {

    def exists: Transaction = x.isEmpty

  }

  // Field Operations.
  implicit class FieldOps(x: Field) {

    def +=(y: Transaction)(implicit ctx: Context): Unit = x.owner.updateDynamic(x.name)(x + y)
    def -=(y: Transaction)(implicit ctx: Context): Unit = x.owner.updateDynamic(x.name)(x - y)
    def *=(y: Transaction)(implicit ctx: Context): Unit = x.owner.updateDynamic(x.name)(x * y)
    def /=(y: Transaction)(implicit ctx: Context): Unit = x.owner.updateDynamic(x.name)(x / y)
    def %=(y: Transaction)(implicit ctx: Context): Unit = x.owner.updateDynamic(x.name)(x % y)
    def ++=(y: Transaction)(implicit ctx: Context): Unit = x.owner.updateDynamic(x.name)(x ++ y)

  }

  // Variable Operations.
  implicit class VariableOps(x: Variable) {

    def +=(y: Transaction)(implicit ctx: Context): Unit = ctx += store(x.name, x + y)
    def -=(y: Transaction)(implicit ctx: Context): Unit = ctx += store(x.name, x - y)
    def *=(y: Transaction)(implicit ctx: Context): Unit = ctx += store(x.name, x * y)
    def /=(y: Transaction)(implicit ctx: Context): Unit = ctx += store(x.name, x / y)
    def %=(y: Transaction)(implicit ctx: Context): Unit = ctx += store(x.name, x % y)
    def ++=(y: Transaction)(implicit ctx: Context): Unit = ctx += store(x.name, x ++ y)

  }

}
