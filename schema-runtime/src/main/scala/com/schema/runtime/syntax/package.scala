package com.schema.runtime

import com.schema.runtime
import com.schema.runtime.syntax.Context.Variable

package object syntax extends Language {

  // Internal delimiter constants.
  val FieldDelimiter: String = "@"
  val ArrayDelimiter: String = ","

  // Implicit conversions.
  implicit def str2lit(value: String): Literal = literal(value)
  implicit def num2lit[T](value: T)(implicit num: Numeric[T]): Literal = literal(value)
  implicit def bol2lit(value: Boolean): Literal = literal(value)
  implicit def pxy2txn(proxy: Proxy): Transaction = read(proxy.key)
  implicit def var2txn(variable: Variable): Transaction = load(variable.name)
  implicit def pxy2fix(proxy: Proxy): InfixTransaction = pxy2txn(proxy)
  implicit def var2fix(variable: Variable): InfixTransaction = var2txn(variable)
  implicit def fld2obj(field: Field): Object = Object(read(field.key))

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

  // Infix Operator Extensions.
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
    def ===(y: Transaction): Transaction = equal(x, y)
    def <>(y: Transaction): Transaction = not(equal(x, y))
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
    def substring(l: Transaction, h: Transaction): Transaction = slice(x, l, h)

  }

}
