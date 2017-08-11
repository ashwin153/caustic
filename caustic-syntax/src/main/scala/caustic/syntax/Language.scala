package caustic.syntax

import Language._

trait Language {

  /**
   * Conditionally branches on the result of the specified condition. An else clause may be
   * optionally specified. Utilizes structural types, which can be enabled by importing the
   * scala.language.reflectiveCalls language feature.
   *
   * @param condition Condition to branch on.
   * @param success Execute if condition is satisfied.
   * @param ctx Implicit transaction context.
   * @return Optional else clause.
   */
  def If(condition: Transaction)(success: => Unit)(
    implicit ctx: Context
  ) = new {
    private val before = ctx.txn
    ctx.txn = Empty
    success
    private val pass = ctx.txn
    ctx.txn = before
    ctx += branch(condition, pass, Empty)

    def Else(failure: => Unit): Unit = {
      ctx.txn = Empty
      failure
      val fail = ctx.txn
      ctx.txn = before
      ctx += branch(condition, pass, fail)
    }
  }

  /**
   * Repeatedly performs the specified block while the condition is satisfied.
   *
   * @param condition Condition to loop on.
   * @param block Loop body.
   * @param ctx Implicit transaction context.
   */
  def While(condition: Transaction)(block: => Unit)(
    implicit ctx: Context
  ): Unit = {
    val before = ctx.txn
    ctx.txn = Empty
    block
    val body = ctx.txn
    ctx.txn = before
    ctx.append(repeat(condition, body))
  }

  /**
   * Repeatedly performs the specified block over the interval, storing the value of the loop index
   * in the provided variable.
   *
   * @param variable Loop variable.
   * @param in Loop interval.
   * @param block Loop body.
   * @param ctx Implicit transaction context.
   */
  def For(variable: Variable, in: Interval)(block: => Unit)(
    implicit ctx: Context
  ): Unit = {
    ctx.append(store(variable.name, in.start))
    val condition = in match {
      case Interval(_, end, _, true)  => load(variable.name) <= end
      case Interval(_, end, _, false) => load(variable.name) <  end
    }

    val before = ctx.txn
    ctx.txn = Empty
    block
    ctx.append(store(variable.name, load(variable.name) + in.step))
    val body = ctx.txn
    ctx.txn = before
    ctx.append(repeat(condition, body))
  }

  /**
   * Returns the value of the specified transaction or transactions.
   *
   * @param first First transaction.
   * @param rest Optional other transactions.
   * @param ctx Implicit transaction context.
   */
  def Return(first: Transaction, rest: Transaction*)(
    implicit ctx: Context
  ): Unit =
    if (rest.isEmpty)
      ctx.append(first)
    else
      ctx.append(concat("[", concat(
        rest.+:(first)
          .map(t => concat("\"", concat(t, "\"")))
          .reduceLeft((a, b) => a ++ "," ++ b),
        "]"
      )))

}

object Language {

  /**
   * A loop interval.
   *
   * @param start Starting value.
   * @param end Ending value.
   * @param step Iteration step size.
   * @param inclusive Whether or not end value is inclusive.
   */
  case class Interval(
    start: Transaction,
    end: Transaction,
    step: Transaction,
    inclusive: Boolean
  ) {

    def by(s: Transaction): Interval = this.copy(step=s)

  }

}