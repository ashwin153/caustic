package caustic

import syntax.ops._
import runtime.thrift._
import runtime.thrift.Operator._

import scala.collection.JavaConverters._
import scala.language.implicitConversions

package object syntax {

  private def literal(x: Literal): Transaction =
    Transaction.literal(x)
  private def expr(op: Operator, args: Transaction*): Transaction =
    Transaction.expression(new Expression(op, args.asJava))

  // Operations.
  implicit def txn2ops(x: Transaction): TransactionOps = TransactionOps(x)
  //  implicit def obj2txn(obj: Object): Transaction = read(obj.key)
  //  implicit def obj2ops(obj: Object): TransactionOps = obj2txn(obj)
  //  implicit def var2txn(variable: Variable): Transaction = load(variable.name)
  //  implicit def var2ops(variable: Variable): TransactionOps = var2txn(variable)
  //  implicit def rng2int(range: Range): Interval = Interval(range.start, range.end, range.step, range.isInclusive)

  // Literal Values.
  def flag(x: Boolean): Transaction = literal(Literal.flag(x))
  implicit def boolean2flag(x: Boolean): Transaction = flag(x)
  def real[T](x: T)(implicit num: Numeric[T]): Transaction = literal(Literal.real(num.toDouble(x)))
  implicit def numeric2real[T](x: T)(implicit num: Numeric[T]): Transaction = real(x)
  def text(x: String): Transaction = literal(Literal.text(x))
  implicit def string2text(x: String): Transaction = text(x)

  // Core Expressions.
  def read(k: Transaction): Transaction = expr(READ, k)
  def write(k: Transaction, v: Transaction): Transaction = expr(WRITE, k, v)
  def load(n: Transaction): Transaction = expr(LOAD, n)
  def cons(f: Transaction, r: Transaction*): Transaction = r.foldLeft(f)((a, b) => expr(CONS, a, b))
  def store(n: Transaction, v: Transaction): Transaction = expr(STORE, n, v)

  def prefetch(k: Transaction): Transaction = expr(PREFETCH, k)
  def repeat(c: Transaction, b: Transaction): Transaction = expr(REPEAT, c, b)
  def branch(c: Transaction, p: Transaction, f: Transaction): Transaction = expr(BRANCH, c, p, f)
  def rollback(r: Transaction): Transaction = expr(ROLLBACK, r)

  // Math Expressions.
  lazy val Zero: Transaction = real(0)
  lazy val One: Transaction = real(1)
  lazy val Two: Transaction = real(2)
  lazy val Half: Transaction = real(0.5)
  lazy val E: Transaction = real(math.E)
  lazy val Pi: Transaction = real(math.Pi)

  def add(x: Transaction, y: Transaction): Transaction = expr(ADD, x, y)
  def sub(x: Transaction, y: Transaction): Transaction = expr(SUB, x, y)
  def mul(x: Transaction, y: Transaction): Transaction = expr(MUL, x, y)
  def div(x: Transaction, y: Transaction): Transaction = expr(DIV, x, y)
  def mod(x: Transaction, y: Transaction): Transaction = expr(MOD, x, y)
  def pow(x: Transaction, y: Transaction): Transaction = expr(POW, x, y)
  def log(x: Transaction): Transaction = expr(LOG, x)
  def sin(x: Transaction): Transaction = expr(SIN, x)
  def cos(x: Transaction): Transaction = expr(COS, x)
  def floor(x: Transaction): Transaction = expr(FLOOR, x)

  def abs(x: Transaction): Transaction = branch(less(x, Zero), sub(Zero, x), x)
  def exp(x: Transaction): Transaction = pow(E, x)
  def tan(x: Transaction): Transaction = div(sin(x), cos(x))
  def cot(x: Transaction): Transaction = div(cos(x), sin(x))
  def sec(x: Transaction): Transaction = div(One, cos(x))
  def csc(x: Transaction): Transaction = div(One, sin(x))
  def sinh(x: Transaction): Transaction = div(sub(exp(x), exp(sub(Zero, x))), Two)
  def cosh(x: Transaction): Transaction = div(add(exp(x), exp(sub(Zero, x))), Two)
  def tanh(x: Transaction): Transaction = div(sinh(x), cosh(x))
  def coth(x: Transaction): Transaction = div(cosh(x), sinh(x))
  def sech(x: Transaction): Transaction = div(One, cosh(x))
  def csch(x: Transaction): Transaction = div(One, sinh(x))
  def sqrt(x: Transaction): Transaction = pow(x, Half)
  def ceil(x: Transaction): Transaction = branch(equal(x, floor(x)), x, floor(x) + One)
  def round(x: Transaction): Transaction = branch(less(sub(x, floor(x)), Half), floor(x), ceil(x))

  // String Expressions.
  lazy val Empty: Transaction = text("")
  def contains(x: Transaction, y: Transaction) = expr(CONTAINS, x, y)
  def length(x: Transaction): Transaction = expr(LENGTH, x)
  def slice(x: Transaction, l: Transaction, h: Transaction): Transaction = expr(SLICE, x, l, h)
  def concat(x: Transaction, y: Transaction): Transaction = expr(CONCAT, x, y)
  def matches(x: Transaction, r: Transaction): Transaction = expr(MATCHES, x, r)

  // Comparison Expressions.
  def and(x: Transaction, y: Transaction): Transaction = expr(ADD, x, y)
  def or(x: Transaction, y: Transaction): Transaction = expr(OR, x, y)
  def not(x: Transaction): Transaction = expr(NOT, x)
  def equal(x: Transaction, y: Transaction): Transaction = expr(EQUAL, x, y)
  def less(x: Transaction, y: Transaction): Transaction = expr(LESS, x, y)

}
