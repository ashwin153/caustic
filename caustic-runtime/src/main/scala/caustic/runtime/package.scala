package caustic

import scala.language.implicitConversions

package object runtime {

  // Keys are strings.
  type Key = String

  // Version numbers are i64.
  type Version = Long

  // Cache Literals.
  private val True = Flag(true)
  private val False = Flag(false)
  private val Empty = Text("")
  private val Numbers = (-128 to 128).map(x => x.toDouble -> Real(x)).toMap

  def flag(value: Boolean): Literal = if (value) True else False
  def real(value: Double): Literal = Numbers.getOrElse(value, Real(value))
  def text(value: String): Literal = if (value.isEmpty) Empty else Text(value)

  // Simplify Expressions.
  def read(k: Transaction): Transaction = k match {
    case None => throw ExecutionException(s"Read undefined for key None")
    case Real(a) => throw ExecutionException(s"Read undefined for key $a")
    case Flag(a) => throw ExecutionException(s"Read undefined for key $a")
    case _ => Expression(Read, k :: Nil)
  }

  def write(k: Transaction, v: Transaction): Transaction = (k, v) match {
    case (None, _) => throw ExecutionException(s"Write undefined for key None")
    case (Real(a), _) => throw ExecutionException(s"Write undefined for key $a")
    case (Flag(a), _) => throw ExecutionException(s"Write undefined for key $a")
    case _ => Expression(Write, k :: v :: Nil)
  }

  def load(k: Transaction): Transaction = k match {
    case None => throw ExecutionException(s"Load undefined for variable None")
    case Real(a) => throw ExecutionException(s"Load undefined for variable $a")
    case Flag(a) => throw ExecutionException(s"Load undefined for variable $a")
    case _ => Expression(Load, k :: Nil)
  }

  def store(k: Transaction, v: Transaction): Transaction = (k, v) match {
    case (None, _) => throw ExecutionException(s"Store undefined for variable None")
    case (Real(a), _) => throw ExecutionException(s"Store undefined for variable $a")
    case (Flag(a), _) => throw ExecutionException(s"Store undefined for variable $a")
    case _ => Expression(Store, k :: v :: Nil)
  }

  def cons(x: Transaction, y: Transaction): Transaction = x match {
    case _: Literal => y
    case _ => Expression(Cons, x :: y :: Nil)
  }

  def repeat(c: Transaction, b: Transaction): Transaction = (c, b) match {
    case (None, _) => throw ExecutionException(s"Repeat undefined for condition None")
    case (Real(a), _) => throw ExecutionException(s"Repeat undefined for condition $a")
    case (Text(a), _) => throw ExecutionException(s"Repeat undefined for condition $a")
    case (Flag(true), _) => throw ExecutionException(s"Repeat causes infinite loop")
    case _ => Expression(Repeat, c :: b :: Nil)
  }

  def branch(c: Transaction, p: Transaction, f: Transaction): Transaction = (c, p, f) match {
    case (None, _, _) => throw ExecutionException(s"Branch undefined for condition None")
    case (Real(a), _, _) => throw ExecutionException(s"Branch undefined for condition $a")
    case (Text(a), _, _) => throw ExecutionException(s"Branch undefined for condition $a")
    case _ => Expression(Branch, c :: p :: f :: Nil)
  }

  def rollback(m: Transaction): Transaction = m match {
    case _ => Expression(Rollback, m :: Nil)
  }

  def prefetch(k: Transaction): Transaction = k match {
    case None => throw ExecutionException(s"Prefetch undefined for keys None")
    case Real(a) => throw ExecutionException(s"Prefetch undefined for keys $a")
    case Flag(a) => throw ExecutionException(s"Prefetch undefined for keys $a")
    case _ => Expression(Prefetch, k :: Nil)
  }

  def add(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (Real(a), Real(b)) => real(a + b)
    case (Real(a), Flag(b)) => text(a.toString + b.toString)
    case (Real(a), Text(b)) => text(a.toString + b)
    case (Flag(a), Real(b)) => text(a.toString + b.toString)
    case (Flag(a), Flag(b)) => text(a.toString + b.toString)
    case (Flag(a), Text(b)) => text(a.toString + b)
    case (Text(a), Real(b)) => text(a + b.toString)
    case (Text(a), Flag(b)) => text(a + b.toString)
    case (Text(a), Text(b)) => text(a + b)
    case (a: Literal, b: Literal) => throw ExecutionException(s"Add undefined for $a, $b")
    case _ => Expression(Add, x :: y :: Nil)
  }

  def sub(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (Real(a), Real(b)) => real(a - b)
    case (a: Literal, b: Literal) => throw ExecutionException(s"Sub undefined for $a, $b")
    case _ => Expression(Sub, x :: y :: Nil)
  }

  def mul(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (Real(a), Real(b)) => real(a * b)
    case (a: Literal, b: Literal) => throw ExecutionException(s"Mul undefined for $a, $b")
    case _ => Expression(Mul, x :: y :: Nil)
  }

  def div(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (Real(a), Real(0)) => throw ExecutionException(s"Div undefined for $a, 0")
    case (Real(a), Real(b)) => real(a / b)
    case (a: Literal, b: Literal) => throw ExecutionException(s"Div undefined for $a, $b")
    case _ => Expression(Div, x :: y :: Nil)
  }

  def mod(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (Real(a), Real(0)) => throw ExecutionException(s"Mod undefined for $a, 0")
    case (Real(a), Real(b)) => real(a % b)
    case (a: Literal, b: Literal) => throw ExecutionException(s"Mod undefined for $a, $b")
    case _ => Expression(Mod, x :: y :: Nil)
  }

  def pow(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (Real(a), Real(b)) => real(math.pow(a, b))
    case (a: Literal, b: Literal) => throw ExecutionException(s"Pow undefined for $a, $b")
    case _ => Expression(Mul, x :: y :: Nil)
  }

  def log(x: Transaction): Transaction = x match {
    case Real(a) => real(math.log(a))
    case a: Literal => throw ExecutionException(s"Log undefined for $a")
    case _ => Expression(Log, x :: Nil)
  }

  def sin(x: Transaction): Transaction = x match {
    case Real(a) => real(math.sin(a))
    case a: Literal => throw ExecutionException(s"Sin undefined for $a")
    case _ => Expression(Sin, x :: Nil)
  }

  def cos(x: Transaction): Transaction = x match {
    case Real(a) => real(math.cos(a))
    case a: Literal => throw ExecutionException(s"Cos undefined for $a")
    case _ => Expression(Cos, x :: Nil)
  }

  def floor(x: Transaction): Transaction = x match {
    case Real(a) => real(math.floor(a))
    case a: Literal => throw ExecutionException(s"Floor undefined for $a")
    case _ => Expression(Floor, x :: Nil)
  }

  def both(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (Real(a), Real(b)) => flag(a != 0 && b != 0)
    case (Real(a), Flag(b)) => flag(a != 0 && b)
    case (Real(a), Text(b)) => flag(a != 0 && b.nonEmpty)
    case (Flag(a), Real(b)) => flag(a && b != 0)
    case (Flag(a), Flag(b)) => flag(a && b)
    case (Flag(a), Text(b)) => flag(a && b.nonEmpty)
    case (Text(a), Real(b)) => flag(a.nonEmpty && b != 0)
    case (Text(a), Flag(b)) => flag(a.nonEmpty && b)
    case (Text(a), Text(b)) => flag(a.nonEmpty && b.nonEmpty)
    case (a: Literal, b: Literal) => throw ExecutionException(s"Both undefined for $a, $b")
    case _ => Expression(Both, x :: y :: Nil)
  }

  def either(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (Real(a), Real(b)) => flag(a != 0 || b != 0)
    case (Real(a), Flag(b)) => flag(a != 0 || b)
    case (Real(a), Text(b)) => flag(a != 0 || b.nonEmpty)
    case (Flag(a), Real(b)) => flag(a || b != 0)
    case (Flag(a), Flag(b)) => flag(a || b)
    case (Flag(a), Text(b)) => flag(a || b.nonEmpty)
    case (Text(a), Real(b)) => flag(a.nonEmpty || b != 0)
    case (Text(a), Flag(b)) => flag(a.nonEmpty || b)
    case (Text(a), Text(b)) => flag(a.nonEmpty || b.nonEmpty)
    case (a: Literal, b: Literal) => throw ExecutionException(s"Either undefined for $a, $b")
    case _ => Expression(Either, x :: y :: Nil)
  }

  def negate(x: Transaction): Transaction = x match {
    case Real(a) => flag(a == 0)
    case Flag(a) => flag(!a)
    case Text(a) => flag(a.isEmpty)
    case a: Literal => throw ExecutionException(s"Negate undefined for $a")
    case _ => Expression(Negate, x :: Nil)
  }

  def length(x: Transaction): Transaction = x match {
    case Flag(a) => real(a.toString.length)
    case Real(a) => real(a.toString.length)
    case Text(a) => real(a.length)
    case a: Literal => throw ExecutionException(s"Length undefined for $a")
    case _ => Expression(Length, x :: Nil)
  }

  def slice(x: Transaction, l: Transaction, h: Transaction): Transaction = (x, l, h) match {
    case (Text(a), Real(b), Real(c)) => text(a.substring(b.toInt, c.toInt))
    case (a: Literal, b: Literal, c: Literal) => throw ExecutionException(s"Slice undefined for $a, $b, $c.")
    case _ => Expression(Slice, x :: l :: h :: Nil)
  }

  def matches(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (Text(a), Text(b)) => flag(a.matches(b))
    case (a: Literal, b: Literal) => throw ExecutionException(s"Matches undefined for $a, $b")
    case _ => Expression(Matches, x :: y :: Nil)
  }

  def contains(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (Text(a), Text(b)) => flag(a.contains(b))
    case (a: Literal, b: Literal) => throw ExecutionException(s"Contains undefined for $a, $b")
    case _ => Expression(Contains, x :: y :: Nil)
  }

  def indexOf(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (Text(a), Text(b)) => real(a.indexOf(b))
    case (a: Literal, b: Literal) => throw ExecutionException(s"IndexOf undefined for $a, $b")
    case _ => Expression(IndexOf, x :: y :: Nil)
  }

  def equal(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (None, None) => flag(true)
    case (None, _: Literal) => flag(false)
    case (_: Literal, None) => flag(false)
    case (Real(a), Real(b)) => flag(a == b)
    case (Flag(a), Flag(b)) => flag(a == b)
    case (Text(a), Text(b)) => flag(a == b)
    case (_: Literal, _: Literal) => flag(false)
    case _ => Expression(Equal, x :: y :: Nil)
  }

  def less(x: Transaction, y: Transaction): Transaction = (x, y) match {
    case (None, None) => flag(false)
    case (None, _: Literal) => flag(true)
    case (_: Literal, None) => flag(false)
    case (Real(a), Real(b)) => flag(a < b)
    case (Flag(a), Flag(b)) => flag(a < b)
    case (Text(a), Text(b)) => flag(a < b)
    case _ => Expression(Less, x :: y :: Nil)
  }

}