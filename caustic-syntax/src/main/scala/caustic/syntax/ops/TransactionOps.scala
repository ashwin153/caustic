package caustic.syntax.ops

import caustic.syntax
import caustic.syntax._
import caustic.runtime.thrift.Transaction
import caustic.syntax.Language.Interval

/**
 *
 * @param x
 */
case class TransactionOps(x: Transaction) {

  def unary_- : Transaction = sub(Zero, x)
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

  def to(y: Transaction): Interval = Interval(x, y, One, inclusive = true)
  def until(y: Transaction): Interval = Interval(x, y, One, inclusive = false)

  def ++(y: Transaction): Transaction = concat(x, y)
  def isEmpty: Transaction = x === Empty
  def nonEmpty: Transaction = x <> Empty
  def charAt(i: Transaction): Transaction = slice(x, i, add(i, One))
  def contains(y: Transaction): Transaction = syntax.contains(x, y)
  def endsWith(y: Transaction): Transaction = y === x.substring(syntax.length(x) - syntax.length(y))
  def startsWith(y: Transaction): Transaction = y === x.substring(0, syntax.length(y))
  def matches(y: Transaction): Transaction = syntax.matches(x, y)
  def substring(l: Transaction): Transaction = x.substring(l, syntax.length(x))
  def substring(l: Transaction, h: Transaction): Transaction = slice(x, l, h)
  def length: Transaction = syntax.length(x)

  def indexOf(y: Transaction, from: Transaction = 0): Transaction =
    cons(
      store("$indexOf", -1),
      store("$i", from),
      repeat(
        load("$i") < (syntax.length(x) - syntax.length(y)) && load("$indexOf") < 0,
        branch(
          x.substring(load("$i"), load("$i") + syntax.length(y)) === y,
          store("$indexOf", load("$i")),
          store("$i", load("$i") + 1))),
      load("$indexOf")
    )

}