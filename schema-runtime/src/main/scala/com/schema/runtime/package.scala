package com.schema

import com.schema.runtime.Transaction._
import com.schema.runtime.Transaction.Operation._

package object runtime {

  implicit def bool2literal(value: Boolean): Literal = literal(value)
  implicit def num2literal[T](value: T)(implicit num: Numeric[T]): Literal = literal(value)
  implicit def str2literal(value: String): Literal = literal(value)

  // All keys and values are Strings. Other primitive types like booleans, integers, and arrays may
  // be implemented using only Strings. Fewer primitive types is beneficial, because it reduces the
  // complexity of the Schema runtime. Each key and value are associated with a non-negative
  // revision number that is used to track the sequence of modifications performed on a key.
  type Key = String
  type Revision = Long
  type Value = String

  /**
   * In order to enable concurrent modification of different fields of the same object, the library
   * internally stores fields as independent entries in the database. Therefore, a globally unique
   * key must be assigned to each field. Because users already assign objects a globally unique key
   * when they insert them, a globally unique key may by generated for each of its fields by
   * concatenating its key with a reserved delimiter and the field name.
   *
   * Selection of a good field delimiter is a challenging problem. On one hand, you would like to
   * select a delimiter that is long enough that it does not appear frequently in object keys. On
   * the other hand, you would also like a delimiter that is short because it takes up less space
   * in memory. An important restriction is that neither the Field nor the List Delimiters may
   * contain any regex special characters.
   */
  val FieldDelimiter = literal("@")

  /**
   * In order to enable the storage of lists of keys, the library must be able to determine where
   * one key terminates and another begins. Therefore, no key or field may ever contain the reserved
   * list delimiter.
   */
  val ListDelimiter = literal(",")

  // Transaction builders for the core runtime operators. These functions will rarely be called
  // because there are more convenient ways to express behavior (e.g. add(a, b) vs. a + b); however,
  // these functions provide the foundations for all the other syntactic sugar.
  def literal(value: Boolean) = if (value) Literal.True else Literal.False
  def literal[T](value: T)(implicit num: Numeric[T]) = Literal(value.toString)
  def literal(value: String) = Literal(value)
  def read(key: Transaction) = Operation(Read, List(key))
  def write(key: Transaction, value: Transaction) = Operation(Write, List(key, value))
  def cons(x: Transaction, y: Transaction) = Operation(Cons, List(x, y))
  def add(x: Transaction, y: Transaction) = Operation(Add, List(x, y))
  def sub(x: Transaction, y: Transaction) = Operation(Sub, List(x, y))
  def mul(x: Transaction, y: Transaction) = Operation(Mul, List(x, y))
  def div(x: Transaction, y: Transaction) = Operation(Div, List(x, y))
  def mod(x: Transaction, y: Transaction) = Operation(Mod, List(x, y))
  def pow(x: Transaction, y: Transaction) = Operation(Pow, List(x, y))
  def log(x: Transaction) = Operation(Log, List(x))
  def sin(x: Transaction) = Operation(Sin, List(x))
  def cos(x: Transaction) = Operation(Cos, List(x))
  def floor(x: Transaction) = Operation(Floor, List(x))
  def length(str: Transaction) = Operation(Length, List(str))
  def slice(str: Transaction, low: Transaction, high: Transaction) = Operation(Slice, List(str, low, high))
  def concat(x: Transaction, y: Transaction) = Operation(Concat, List(x, y))
  def branch(cmp: Transaction, pass: Transaction, fail: Transaction) = Operation(Branch, List(cmp, pass, fail))
  def equal(x: Transaction, y: Transaction) = Operation(Equal, List(x, y))
  def matches(str: Transaction, regex: Transaction) = Operation(Matches, List(str, regex))
  def and(x: Transaction, y: Transaction) = Operation(And, List(x, y))
  def not(x: Transaction) = Operation(Not, List(x))
  def or(x: Transaction, y: Transaction) = Operation(Or, List(x, y))
  def less(x: Transaction, y: Transaction) = Operation(Less, List(x, y))
  def purge(list: Transaction) = Operation(Purge, List(list))
  def exists(key: Transaction) = not(equal(read(key), Literal.Empty))

  // Transaction builders for additional math operations. These math functions are special cases or
  // compositions of the core operators defined above. Math operators that are used frequently ought
  // to be implemented directly in the runtime for better performance. However, for most use cases
  // a simpler runtime is preferable to a performant one.
  lazy val E: Transaction = Literal(math.E.toString)
  lazy val Pi: Transaction = Literal(math.Pi.toString)

  def abs(x: Transaction) = branch(less(x, Literal.Zero), sub(Literal.Zero, x), x)
  def tan(x: Transaction) = div(sin(x), cos(x))
  def cot(x: Transaction) = div(cos(x), sin(x))
  def sec(x: Transaction) = div(Literal.One, cos(x))
  def csc(x: Transaction) = div(Literal.One, sin(x))
  def exp(x: Transaction) = pow(E, x)
  def ceil(x: Transaction) = add(floor(x), Literal.One)
  def sqrt(x: Transaction) = pow(x, Literal.Half)
  def sinh(x: Transaction) = div(sub(exp(x), exp(sub(Literal.Zero, x))), Literal.Two)
  def cosh(x: Transaction) = div(add(exp(x), exp(sub(Literal.Zero, x))), Literal.Two)
  def tanh(x: Transaction) = div(sinh(x), cosh(x))
  def coth(x: Transaction) = div(cosh(x), sinh(x))
  def sech(x: Transaction) = div(Literal.One, cosh(x))
  def csch(x: Transaction) = div(Literal.One, sinh(x))
  def round(x: Transaction) = branch(less(sub(x, floor(x)), Literal.Half), floor(x), ceil(x))

  /**
   * An extension of a [[Transaction]] that provides convenient infix operators which simplify
   * transaction construction. Without these implicit extensions, transaction construction is
   * verbose and incomprehensible.
   *
   * @param txn Underlying transaction.
   */
  implicit class RichTransaction(txn: Transaction) {

    def +(that: Transaction) = add(txn, that)
    def -(that: Transaction) = sub(txn, that)
    def *(that: Transaction) = mul(txn, that)
    def /(that: Transaction) = div(txn, that)
    def %(that: Transaction) = mod(txn, that)
    def unary_- = sub(Literal.Zero, txn)

    def ++(that: Transaction) = concat(txn, that)
    def charAt(index: Transaction) = slice(txn, index, add(index, Literal.One))
    def contains(that: Transaction) = matches(txn, concat(".*", concat(that, ".*")))
    def endsWith(that: Transaction) = matches(txn, concat(that, "$"))
    def substring(lower: Transaction) = slice(txn, lower, length(txn))
    def substring(lower: Transaction, upper: Transaction) = slice(txn, lower, upper)
    def startsWith(that: Transaction) = matches(txn, concat("^", that))

    def <(that: Transaction) = less(txn, that)
    def >(that: Transaction) = not(or(equal(txn, that), less(txn, that)))
    def ==(that: Transaction) = equal(txn, that)
    def !=(that: Transaction) = not(equal(txn, that))
    def <=(that: Transaction) = or(equal(txn, that), less(txn, that))
    def >=(that: Transaction) = not(less(txn, that))
    def max(that: Transaction) = branch(less(txn, that), that, txn)
    def min(that: Transaction) = branch(less(txn, that), txn, that)
    def &&(that: Transaction) = and(txn, that)
    def ||(that: Transaction) = or(txn, that)
    def unary_! = not(txn)
    
  }

}
