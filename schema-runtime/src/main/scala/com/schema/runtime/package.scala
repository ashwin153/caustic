package com.schema

import akka.actor.Scheduler
import akka.pattern.after
import com.schema.runtime.Transaction.Operation._
import com.schema.runtime.Transaction.Literal._
import com.schema.runtime.Transaction.{Literal, Operation}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

package object runtime {

  // We may assume without loss of generality that all keys and values are strings, because all
  // digital information must be representable as a string of ones and zeroes. Each key and value is
  // associated with a revision number, which the database uses to detect transaction conflicts.
  type Key = String
  type Revision = Long
  type Value = String

  // Implicit conversions.
  implicit def str2txn(value: String): Transaction = literal(value)
  implicit def bool2txn(value: Boolean): Transaction = literal(value)
  implicit def num2txn[T](value: T)(implicit num: Numeric[T]): Transaction = literal(value)
  implicit def proxy2txn(proxy: Proxy): Transaction = proxy.path
  implicit def proxy2ops(proxy: Proxy): RichTransaction = proxy2txn(proxy)

  // Objects are stored independently from their fields so that different fields of the same object
  // may be simultaneously modified. In order to generate a globally unique key for a field, the
  // library simply concatenates the globally unique object identifier, the reserved field delimiter
  // (which no field or key may contain), and the unique field name.
  val FieldDelimiter: Literal = Literal("@")

  // Deletion of any object is cascaded to all of its various fields. This behavior avoids memory
  // leaks, but requires that the various keys for the fields of an object be persisted somewhere.
  // In order to serialize this list, the reserved list delimiter is inserted between field keys.
  val ListDelimiter: Literal = Literal(",")

  // Core Operations.
  def read(k: Transaction): Transaction = Operation(Read, List(k))
  def write(k: Transaction, v: Transaction): Transaction = Operation(Write, List(k, v))
  def branch(c: Transaction, p: Transaction, f: Transaction): Transaction = Operation(Branch, List(c, p, f))
  def cons(x: Transaction, y: Transaction): Transaction = Operation(Cons, List(x, y))
  def purge(x: Transaction): Transaction = Operation(Purge, List(x))
  def literal(x: Boolean): Transaction = if (x) True else False
  def literal(x: String): Transaction = Literal(x)
  def literal[T] (x: T)(implicit num: Numeric[T]): Transaction = Literal(x.toString)

  // Math Operations.
  lazy val E  : Transaction = Literal(math.E.toString)
  lazy val Pi : Transaction = Literal(math.Pi.toString)

  def add(x: Transaction, y: Transaction): Transaction = Operation(Add, List(x, y))
  def sub(x: Transaction, y: Transaction): Transaction = Operation(Sub, List(x, y))
  def mul(x: Transaction, y: Transaction): Transaction = Operation(Mul, List(x, y))
  def div(x: Transaction, y: Transaction): Transaction = Operation(Div, List(x, y))
  def mod(x: Transaction, y: Transaction): Transaction = Operation(Mod, List(x, y))
  def abs(x: Transaction): Transaction = branch(less(x, Zero), sub(Zero, x), x)

  def pow(x: Transaction, y: Transaction): Transaction = Operation(Pow, List(x, y))
  def exp(x: Transaction): Transaction = pow(E, x)
  def log(x: Transaction): Transaction = Operation(Log, List(x))
  def sqrt(x: Transaction): Transaction = pow(x, Half)

  def sin(x: Transaction): Transaction = Operation(Sin, List(x))
  def cos(x: Transaction): Transaction = Operation(Cos, List(x))
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

  def ceil(x: Transaction): Transaction = add(floor(x), One)
  def floor(x: Transaction): Transaction = Operation(Floor, List(x))
  def round(x: Transaction): Transaction = branch(less(sub(x, floor(x)), Half), floor(x), ceil(x))

  // String Operations.
  def length(x: Transaction): Transaction = Operation(Length, List(x))
  def concat(x: Transaction, y: Transaction): Transaction = Operation(Concat, List(x, y))
  def slice(x: Transaction, l: Transaction, h: Transaction): Transaction = Operation(Slice, List(x, l, h))

  // Logical Operations.
  def and(x: Transaction, y: Transaction): Transaction = Operation(And, List(x, y))
  def or(x: Transaction, y: Transaction): Transaction = Operation(Or, List(x, y))
  def not(x: Transaction): Transaction = Operation(Not, List(x))
  def exists(x: Transaction): Transaction = not(equal(read(x), Empty))
  def equal(x: Transaction, y: Transaction): Transaction = Operation(Equal, List(x, y))
  def less(x: Transaction, y: Transaction): Transaction = Operation(Less, List(x, y))
  def matches(x: Transaction, y: Transaction): Transaction = Operation(Matches, List(x, y))
  def contains(x: Transaction, y: Transaction): Transaction = Operation(Contains, List(x, y))

  // Infix Operations.
  implicit class RichTransaction(x: Transaction) {

    def unary_- : Transaction = sub(Zero, x)
    def unary_! : Transaction = not(x)
    def +(y: Transaction): Transaction = add(x, y)
    def -(y: Transaction): Transaction = sub(x, y)
    def *(y: Transaction): Transaction = mul(x, y)
    def /(y: Transaction): Transaction = div(x, y)
    def %(y: Transaction): Transaction = mod(x, y)
    def <(y: Transaction): Transaction = less(x, y)
    def >(y: Transaction): Transaction = not(or(equal(x, y), less(x, y)))
    def ==(y: Transaction): Transaction = equal(x, y)
    def !=(y: Transaction): Transaction = not(equal(x, y))
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
    def substring(l: Transaction, h: Transaction): Transaction = slice(x, l.min(h).max(0), h.max(l).min(length(x)))

  }

  /**
   * Asynchronously executes the transaction generated by the specified function and returns the
   * result of execution.
   *
   * @param f Transaction generator.
   * @param db Implicit database.
   * @param ec Implicit execution context.
   * @return Result of transaction execution, or an exception on failure.
   */
  def Schema(f: Context => Unit)(
    implicit ec: ExecutionContext,
    db: Database
  ): Future[String] = {
    val builder = Context(Literal.Empty)
    f(builder)
    db.execute(builder.txn)
  }

  /**
   * Asynchronously executes the transaction generated by the specified function, returns the result
   * of execution, and automatically retries failures with the specified backoff. Implementation
   * relies on Akka to schedule retries, and is fully compatible with the backoff strategies
   * implemented in Backoff.scala within the Finagle project.
   *
   * @param backoffs Backoff durations.
   * @param f Transaction generator.
   * @param ec Implicit execution context.
   * @param scheduler Implicit Akka scheduler.
   * @param db Implicit database.
   * @return Result of transaction execution, or an exception on retried failure.
   */
  def Schema(backoffs: Stream[FiniteDuration])(f: Context => Unit)(
    implicit ec: ExecutionContext,
    scheduler: Scheduler,
    db: Database
  ): Future[String] =
    Schema(f).recoverWith { case _ if backoffs.nonEmpty =>
      after(backoffs.head, scheduler)(Schema(backoffs.drop(1))(f))
    }

  /**
   * Returns a proxy to the object specified by its globally unique identifier.
   *
   * @param key Globally unique identifier.
   * @param ctx Implicit transaction context.
   * @return Proxy to the requested object.
   */
  def Select(key: Key)(implicit ctx: Context): Proxy = {
    require(!key.contains(FieldDelimiter), "Key may not contain field delimiter.")
    require(!key.contains(ListDelimiter), "Key may not contain list delimiter.")
    new Proxy(key)
  }

  /**
   * Deletes the specified object and its various fields. Deletion requires each object to be mapped
   * to the list of its field names so that they may be efficiently purged. Furthermore, each field
   * modification always requires an additional read to verify that the field name is recorded in
   * the list of the corresponding identifier and may require an additional write in case it has not
   * been already recorded. However, the additional read is inexpensive (at least one read is always
   * performed, and reads are batched together) and the additional write is rarely necessary because
   * most applications have relatively static data models. Delete should only be called on reference
   * types, if delete is called on a field is has undetermined behavior.
   *
   * @param proxy Object to delete.
   * @param ctx Implicit transaction context.
   */
  def Delete(proxy: Proxy)(implicit ctx: Context): Unit = {
    ctx :+ purge(read(proxy))
    ctx :+ write(proxy.path, Literal.Empty)
  }

  /**
   * Conditionally branches to the first block if the specified comparison is non-empty and to the
   * second otherwise. Implementation relies on structural types (duck typing), and so the language
   * feature scala.language.reflectiveCalls must be in scope to silence compiler warnings.
   *
   * @param cmp Comparison condition.
   * @param success Required If clause.
   * @param ctx Implicit transaction context.
   * @return Optional Else clause.
   */
  def If(cmp: Transaction)(success: => Unit)(implicit ctx: Context) = new {
    private val before = ctx.txn
    ctx.txn = Empty
    success
    private val pass = ctx.txn
    ctx.txn = before
    ctx :+ branch(cmp, pass, Empty)

    def Else(failure: => Unit): Unit = {
      ctx.txn = Empty
      failure
      val fail = ctx.txn
      ctx.txn = before
      ctx :+ branch(cmp, pass, fail)
    }
  }

  /**
   * Appends the value of the specified transactions to the transaction builder. Values are
   * concatenated together into a json array for convenience.
   *
   * @param first First transaction to return.
   * @param rest Other transactions to return.
   * @param ctx Implicit transaction context.
   */
  def Return(first: Transaction, rest: Transaction*)(implicit ctx: Context): Unit =
    ctx :+ concat("[", concat(
      rest.+:(first)
        .map(t => concat("\"", concat(t, "\"")))
        .reduceLeftOption((a, b) => a ++ "," ++ b)
        .getOrElse(first),
      "]"
    ))

}
