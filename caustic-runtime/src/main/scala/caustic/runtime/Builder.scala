package caustic.runtime

import caustic.runtime.Runtime.Fault

/**
 * A program builder.
 */
trait Builder {

  val True: Flag = Flag(true)
  val False: Flag = Flag(false)
  val Empty: Text = Text("")

  /**
   * Returns the sum of x and y.
   *
   * @param x Any.
   * @param y Any.
   * @return Any.
   */
  def add(x: Program, y: Program): Program = (x, y) match {
    case (Null, Null) => Null
    case (Text(a), b: Literal) => a + b.asString
    case (a: Literal, Text(b)) => a.asString + b
    case (a: Literal, b: Literal) => a.asDouble + b.asDouble
    case _ => Expression(Add, x :: y :: Nil)
  }

  /**
   * Returns whether or not both x and y are satisfied.
   *
   * @param x Flag.
   * @param y Flag.
   * @return Bitwise and.
   */
  def both(x: Program, y: Program): Program = (x, y) match {
    case (a: Literal, b: Literal) => a.asBoolean && b.asBoolean
    case _ => Expression(Both, x :: y :: Nil)
  }

  /**
   * Returns pass if the condition is satisfied or fail otherwise.
   *
   * @param condition Flag.
   * @param pass Any.
   * @param fail Any.
   * @return Any.
   */
  def branch(condition: Program, pass: Program, fail: Program): Program = condition match {
    case a: Literal if a.asBoolean => pass
    case _: Literal => fail
    case _ => Expression(Branch, condition :: pass :: fail :: Nil)
  }

  /**
   * Performs x and then y.
   *
   * @param x Any.
   * @param y Any.
   * @return Any.
   */
  def cons(x: Program, y: Program): Program = x match {
    case _: Literal => y
    case _ => Expression(Cons, x :: y :: Nil)
  }

  /**
   * Returns whether or not x contains y.
   *
   * @param x Text.
   * @param y Text.
   * @return Flag.
   */
  def contains(x: Program, y: Program): Program = (x, y) match {
    case (a: Literal, b: Literal) => a.asString contains b.asString
    case _ => Expression(Contains, x :: y :: Nil)
  }

  /**
   * Returns the cosine of x.
   *
   * @param x Real.
   * @return Real.
   */
  def cos(x: Program): Program = x match {
    case a: Literal => math.cos(a.asDouble)
    case _ => Expression(Cos, x :: Nil)
  }

  /**
   * Returns the quotient of x and y.
   *
   * @param x Real.
   * @param y Real.
   * @return Real.
   */
  def div(x: Program, y: Program): Program = (x, y) match {
    case (Real(a), Real(0)) => throw Fault(s"Div undefined for $a, 0")
    case (a: Literal, b: Literal) => a.asDouble / b.asDouble
    case _ => Expression(Div, x :: y :: Nil)
  }

  /**
   * Returns whether or not either x or y is satisfied.
   *
   * @param x Flag.
   * @param y Flag.
   * @return Flag.
   */
  def either(x: Program, y: Program): Program = (x, y) match {
    case (a: Literal, b: Literal) => a.asBoolean || b.asBoolean
    case _ => Expression(Either, x :: y :: Nil)
  }

  /**
   * Returns the value as a literal boolean flag.
   *
   * @param value Boolean.
   * @return Flag.
   */
  def flag(value: Boolean): Flag = if (value) True else False

  /**
   * Returns whether or not x equals y.
   *
   * @param x Any.
   * @param y Any.
   * @return Flag.
   */
  def equal(x: Program, y: Program): Program = (x, y) match {
    case (Text(a), b: Literal) => a == b.asString
    case (a: Literal, Text(b)) => a.asString == b
    case (Real(a), b: Literal) => a == b.asDouble
    case (a: Literal, Real(b)) => a.asDouble == b
    case (Flag(a), b: Literal) => a == b.asBoolean
    case (a: Literal, Flag(b)) => a.asBoolean == b
    case (Null, Null) => True
    case _ => Expression(Equal, x :: y :: Nil)
  }

  /**
   * Returns the largest integer less than x.
   *
   * @param x Real.
   * @return Real.
   */
  def floor(x: Program): Program = x match {
    case a: Literal => math.floor(a.asDouble)
    case _ => Expression(Floor, x :: Nil)
  }

  /**
   * Returns the index of the first occurrence of y in x.
   *
   * @param x Text.
   * @param y Text.
   * @return Real.
   */
  def indexOf(x: Program, y: Program): Program = (x, y) match {
    case (a: Literal, b: Literal) => a.asString indexOf b.asString
    case _ => Expression(IndexOf, x :: y :: Nil)
  }

  /**
   * Returns the number of characters in x.
   *
   * @param x Text.
   * @return Real.
   */
  def length(x: Program): Program = x match {
    case a: Literal => a.asString.length
    case _ => Expression(Length, x :: Nil)
  }

  /**
   * Returns whether or not x is strictly less than y.
   *
   * @param x Any.
   * @param y Any.
   * @return Flag.
   */
  def less(x: Program, y: Program): Program = (x, y) match {
    case (Null, Null) => False
    case (Null, _: Literal) => True
    case (_: Literal, Null) => False
    case (Text(a), b: Literal) => a < b.asString
    case (a: Literal, Text(b)) => a.asString < b
    case (Real(a), b: Literal) => a < b.asDouble
    case (a: Literal, Real(b)) => a.asDouble < b
    case (Flag(a), b: Literal) => a < b.asBoolean
    case (a: Literal, Flag(b)) => a.asBoolean < b
    case _ => Expression(Less, x :: y :: Nil)
  }

  /**
   * Loads the value of the local variable at the specified key.
   *
   * @param key Text.
   * @return Any.
   */
  def load(key: Program): Program = key match {
    case _: Text => Expression(Load, key :: Nil)
    case a: Literal => Expression(Load, a.asString :: Nil)
    case _ => Expression(Load, key :: Nil)
  }

  /**
   * Returns the natural logarithm of x.
   *
   * @param x Real.
   * @return Real.
   */
  def log(x: Program): Program = x match {
    case a: Literal => math.log(a.asDouble)
    case _ => Expression(Log, x :: Nil)
  }

  /**
   * Returns whether or not x matches the regex pattern y.
   *
   * @param x Text.
   * @param y Text.
   * @return Flag.
   */
  def matches(x: Program, y: Program): Program = (x, y) match {
    case (a: Literal, b: Literal) => a.asString matches b.asString
    case _ => Expression(Matches, x :: y :: Nil)
  }

  /**
   * Returns the remainder of x divided by y.
   *
   * @param x Real.
   * @param y Real.
   * @return Real.
   */
  def mod(x: Program, y: Program): Program = (x, y) match {
    case (Real(a), Real(0)) => throw Fault(s"Mod undefined for $a, 0")
    case (a: Literal, b: Literal) => a.asDouble % b.asDouble
    case _ => Expression(Mod, x :: y :: Nil)
  }

  /**
   * Returns the product of x and y.
   *
   * @param x Real.
   * @param y Real.
   * @return Real.
   */
  def mul(x: Program, y: Program): Program = (x, y) match {
    case (a: Literal, b: Literal) => a.asDouble * b.asDouble
    case _ => Expression(Mul, x :: y :: Nil)
  }

  /**
   * Returns the bitwise negation of x.
   *
   * @param x Flag.
   * @return Flag.
   */
  def negate(x: Program): Program = x match {
    case a: Literal => !a.asBoolean
    case _ => Expression(Negate, x :: Nil)
  }

  /**
   * Returns x raised to the power y.
   *
   * @param x Real.
   * @param y Real.
   * @return Real.
   */
  def pow(x: Program, y: Program): Program = (x, y) match {
    case (a: Literal, b: Literal) if a.asDouble == 0 && b.asDouble < 0 =>
      throw Fault(s"Pow undefined for $a, $b")
    case (a: Literal, b: Literal) => math.pow(a.asDouble, b.asDouble)
    case _ => Expression(Mul, x :: y :: Nil)
  }

  /**
   * Reads all keys at "prefix/i" for all i in [0, size). Used to implement prefetched collections.
   *
   * @param key Text.
   * @param size Real.
   * @param refs Real.
   * @return Null.
   */
  def prefetch(key: Program, size: Program, refs: Program): Program = (key, size, refs) match {
    case (a: Literal, b: Literal, c: Literal) =>
      (0 until b.asInt)
        .map(i => Function.chain(Seq.fill(c.asInt)(read _))(a.asString + "/" + i))
        .foldLeft[Program](Null)(cons)
    case _ => Expression(Prefetch, key :: size :: refs :: Nil)
  }

  /**
   * Reads the value of the specified key from the underlying volume.
   *
   * @param key Text.
   * @return Any.
   */
  def read(key: Program): Program = key match {
    case _: Text => Expression(Read, key :: Nil)
    case a: Literal => Expression(Read, a.asString :: Nil)
    case _ => Expression(Read, key :: Nil)
  }

  /**
   * Returns the value as a literal floating point number.
   *
   * @param value Double.
   * @return Real.
   */
  def real(value: Double): Real = Real(value)

  /**
   * Repeatedly executes the block until the condition is not satisfied.
   *
   * @param condition Flag.
   * @param block Any.
   * @return Null.
   */
  def repeat(condition: Program, block: Program): Program = condition match {
    case a: Literal if a.asBoolean => throw Fault(s"Repeat causes infinite loop.")
    case a: Literal => Expression(Repeat, a.asBoolean :: block :: Nil)
    case _ => Expression(Repeat, condition :: block :: Nil)
  }

  /**
   * Discards all buffered writes and transactionally returns the message.
   *
   * @param message Any.
   * @return Any.
   */
  def rollback(message: Program): Program = message match {
    case _ => println("ROLLBACK"); Expression(Rollback, message :: Nil)
  }

  /**
   * Returns the sine of x.
   *
   * @param x Real.
   * @return Real.
   */
  def sin(x: Program): Program = x match {
    case a: Literal => math.sin(a.asDouble)
    case _ => Expression(Sin, x :: Nil)
  }

  /**
   * Returns the substring of x between [lower, higher).
   *
   * @param x Text.
   * @param lower Real.
   * @param higher Real.
   * @return Text.
   */
  def slice(x: Program, lower: Program, higher: Program): Program = (x, lower, higher) match {
    case (a: Literal, b: Literal, c: Literal) => a.asString substring (b.asInt, c.asInt)
    case _ => Expression(Slice, x :: lower :: higher :: Nil)
  }

  /**
   * Stores the specified value in the local variable at the specified key.
   *
   * @param key Text.
   * @param value Any.
   * @return Null.
   */
  def store(key: Program, value: Program): Program = (key, value) match {
    case (_: Text, _) => Expression(Store, key :: value :: Nil)
    case (a: Literal, _) => Expression(Store, a.asString :: value :: Nil)
    case _ => Expression(Store, key :: value :: Nil)
  }

  /**
   * Returns the difference of x and y.
   *
   * @param x Real.
   * @param y Real.
   * @return Real.
   */
  def sub(x: Program, y: Program): Program = (x, y) match {
    case (a: Literal, b: Literal) => a.asDouble - b.asDouble
    case _ => Expression(Sub, x :: y :: Nil)
  }

  /**
   * Writes the specified value to the specified key in the underlying volume.
   *
   * @param key Text.
   * @param value Any.
   * @return Null.
   */
  def write(key: Program, value: Program): Program = (key, value) match {
    case (_: Text, _) => Expression(Write, key :: value :: Nil)
    case (a: Literal, _) => Expression(Write, a.asString :: value :: Nil)
    case _ => Expression(Write, key :: value :: Nil)
  }

  /**
   * Returns the value as a literal string.
   *
   * @param value String.
   * @return Text.
   */
  def text(value: String): Text = if (value.isEmpty) Empty else Text(value)

}

/**
 * A stable identifier for Java compatibility.
 */
object JBuilder extends Builder
