package caustic.lang

import caustic.runtime.typing._
import caustic.runtime._

package object control {

  /**
   * Branches on the value of the specified condition. An Else clause may be optionally specified.
   * Utilizes structural types, which can be enabled by importing the scala.language.reflectiveCalls
   * language feature.
   *
   * @param condition Condition to branch on.
   * @param success Execute if condition is satisfied.
   * @param context Implicit transaction context.
   * @return Optional else clause.
   */
  def If[T](condition: Value[caustic.runtime.typing.Boolean])(success: => Value[T])(implicit context: Context) = new {
    private val before = context.body
    context.body = Null
    success
    private val pass = context.body
    context.body = before
    context += branch(condition, pass, Null)

    def Else(failure: => Value[T]): Value[T] = {
      context.body = Null
      val result = failure
      val fail = context.body
      context.body = before
      context += branch(condition, pass, fail)
      result
    }
  }

  /**
   * Performs the block while the condition is satisfied. Because loops are evaluated one iteration
   * at a time by the runtime, it is important to prefetch keys before entering the body of the
   * loop. Otherwise, each iteration of the loop will require a separate read on the database.
   *
   * @param condition Condition to loop on.
   * @param block Loop body.
   * @param context Implicit transaction context.
   */
  def While(condition: Value[caustic.runtime.typing.Boolean])(block: => Unit)(implicit context: Context): Unit = {
    val before = context.body
    context.body = Null
    block
    val body = context.body
    context.body = before
    context += repeat(condition, body)
  }

  /**
   *
   * @param result
   * @param context
   * @tparam T
   */
  def Rollback[T <: Primitive](result: Value[T])(implicit context: Context): Unit = {
    context += rollback(result)
  }

}
