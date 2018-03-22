package caustic.library

import caustic.library.typing._
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
  def If[T](condition: Value[Boolean])(success: => T)(implicit context: Context) = new {
    private val before = context.body
    context.body = Null
    success
    private val pass = context.body
    context.body = before
    context += branch(condition, pass, Null)

    def Else(failure: => T): T = {
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
  def While(condition: Value[Boolean])(block: => Unit)(implicit context: Context): Unit = {
    val before = context.body
    context.body = Null
    block
    val body = context.body
    context.body = before
    context += repeat(condition, body)
  }

  /**
   * Rollsback the transaction and returns the specified result. All modifications made by a program
   * are discarded on rollback.
   *
   * @param result Return value.
   * @param context Parsing context.
   */
  def Rollback[T <: Primitive](result: Value[T])(implicit context: Context): Unit = {
    context += rollback(result)
  }

}
