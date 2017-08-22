package caustic.syntax
package ops

import Language._
import caustic.syntax

/**
 *
 * @param x
 */
case class TransactionOps(x: Transaction) {

  // Math Operations.
  def unary_- : Transaction = sub(Zero, x)
  def +(y: Transaction): Transaction = add(x, y)
  def -(y: Transaction): Transaction = sub(x, y)
  def *(y: Transaction): Transaction = mul(x, y)
  def /(y: Transaction): Transaction = div(x, y)
  def %(y: Transaction): Transaction = mod(x, y)

  // Comparison Operations.
  def unary_! : Transaction = negate(x)
  def unary_~ : Transaction = negate(x)
  def <(y: Transaction): Transaction = less(x, y)
  def >(y: Transaction): Transaction = negate(either(equal(x, y), less(x, y)))
  def <>(y: Transaction): Transaction = negate(equal(x, y))
  def <=(y: Transaction): Transaction = either(equal(x, y), less(x, y))
  def >=(y: Transaction): Transaction = negate(less(x, y))
  def &&(y: Transaction): Transaction = both(x, y)
  def ||(y: Transaction): Transaction = either(x, y)
  def ===(y: Transaction): Transaction = equal(x, y)
  def max(y: Transaction): Transaction = branch(less(x, y), y, x)
  def min(y: Transaction): Transaction = branch(less(x, y), x, y)

  // String Operations.
  def ++(y: Transaction): Transaction = concat(x, y)
  def charAt(i: Transaction): Transaction = slice(x, i, add(i, One))
  def isEmpty: Transaction = x === Empty
  def nonEmpty: Transaction = x <> Empty
  def size: Transaction = syntax.length(x)
  def length: Transaction = syntax.length(x)
  def contains(y: Transaction): Transaction = syntax.contains(x, y)
  def matches(y: Transaction): Transaction = syntax.matches(x, y)
  def substring(l: Transaction): Transaction = x.substring(l, this.length)
  def substring(l: Transaction, h: Transaction): Transaction = slice(x, l, h)
  def endsWith(y: Transaction): Transaction = y === x.substring(this.length - y.length)
  def startsWith(y: Transaction): Transaction = y === x.substring(0, y.length)
  def indexOf(y: Transaction): Transaction = syntax.indexOf(x, y)

}