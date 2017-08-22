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
    ctx += repeat(condition, body)
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
      ctx += first
    else
      ctx += concat("[", concat(
        rest.+:(first)
          .map(t => concat("\"", concat(t, "\"")))
          .reduceLeft((a, b) => a ++ "," ++ b),
        "]"
      ))

}