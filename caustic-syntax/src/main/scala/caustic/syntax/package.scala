package caustic

import caustic.runtime.thrift
import caustic.runtime.thrift._
import caustic.syntax.Language._
import caustic.syntax.ops._

import scala.language.implicitConversions

package object syntax extends Language {

  type Transaction = thrift.Transaction

  // Codec.
  implicit def lit2txn(x: Literal): Transaction = thrift.Transaction.literal(x)
  implicit def exp2txn(x: Expression): Transaction = thrift.Transaction.expression(x)

  // Language.
  implicit def txn2ops(x: Transaction): TransactionOps = TransactionOps(x)
  implicit def var2txn(x: Variable): Transaction = load(x.name)
  implicit def var2top(x: Variable): TransactionOps = TransactionOps(x)
  implicit def rec2txn(x: Record): Transaction = read(x.key)
  implicit def rec2top(x: Record): TransactionOps = TransactionOps(x)
  implicit def rec2ops(x: Record): RecordOps = RecordOps(x)
  implicit def rng2int(x: Range): Interval = Interval(x.start, x.end, x.step, x.isInclusive)

  // Literal Values.
  def flag(x: Boolean): Transaction = Literal.flag(x)
  implicit def boolean2flag(x: Boolean): Transaction = flag(x)
  def real[T](x: T)(implicit num: Numeric[T]): Transaction = Literal.real(num.toDouble(x))
  implicit def numeric2real[T](x: T)(implicit num: Numeric[T]): Transaction = real(x)
  def text(x: String): Transaction = Literal.text(x)
  implicit def string2text(x: String): Transaction = text(x)

  // Core Expressions.
  def read(k: Transaction): Transaction = Expression.read(new Read(k))
  def write(k: Transaction, v: Transaction): Transaction = Expression.write(new Write(k, v))
  def load(n: Transaction): Transaction = Expression.load(new Load(n))
  def store(n: Transaction, v: Transaction): Transaction = Expression.store(new Store(n, v))
  def branch(c: Transaction, p: Transaction, f: Transaction): Transaction = Expression.branch(new Branch(c, p, f))
  def cons(a: Transaction, b: Transaction): Transaction = Expression.cons(new Cons(a, b))
  def repeat(c: Transaction, b: Transaction): Transaction = Expression.repeat(new Repeat(c, b))
  def prefetch(k: Transaction): Transaction = Expression.prefetch(new Prefetch(k))
  def rollback(r: Transaction): Transaction = Expression.rollback(new Rollback(r))

  // Math Expressions.
  lazy val Zero: Transaction = real(0)
  lazy val One: Transaction = real(1)
  lazy val Two: Transaction = real(2)
  lazy val Half: Transaction = real(0.5)
  lazy val E: Transaction = real(math.E)
  lazy val Pi: Transaction = real(math.Pi)

  def add(x: Transaction, y: Transaction): Transaction = Expression.add(new Add(x, y))
  def sub(x: Transaction, y: Transaction): Transaction = Expression.sub(new Sub(x, y))
  def mul(x: Transaction, y: Transaction): Transaction = Expression.mul(new Mul(x, y))
  def div(x: Transaction, y: Transaction): Transaction = Expression.div(new Div(x, y))
  def mod(x: Transaction, y: Transaction): Transaction = Expression.mod(new Mod(x, y))
  def pow(x: Transaction, y: Transaction): Transaction = Expression.pow(new Pow(x, y))
  def log(x: Transaction): Transaction = Expression.log(new Log(x))
  def sin(x: Transaction): Transaction = Expression.sin(new Sin(x))
  def cos(x: Transaction): Transaction = Expression.cos(new Cos(x))
  def floor(x: Transaction): Transaction = Expression.floor(new Floor(x))

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
  def contains(x: Transaction, y: Transaction): Transaction = Expression.contains(new Contains(x, y))
  def length(x: Transaction): Transaction = Expression.length(new Length(x))
  def concat(x: Transaction, y: Transaction): Transaction = add(x, y)
  def slice(x: Transaction, l: Transaction, h: Transaction): Transaction = Expression.slice(new Slice(x, l, h))
  def matches(x: Transaction, r: Transaction): Transaction = Expression.matches(new Matches(x, r))
  def indexOf(x: Transaction, y: Transaction): Transaction = Expression.indexOf(new IndexOf(x, y))

  // Comparison Expressions.
  def both(x: Transaction, y: Transaction): Transaction = Expression.both(new Both(x, y))
  def either(x: Transaction, y: Transaction): Transaction = Expression.either(new Either(x, y))
  def negate(x: Transaction): Transaction = Expression.negate(new Negate(x))
  def equal(x: Transaction, y: Transaction): Transaction = Expression.equal(new Equal(x, y))
  def less(x: Transaction, y: Transaction): Transaction = Expression.less(new Less(x, y))

}
