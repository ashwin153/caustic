package com.schema.runtime
package syntax

import com.schema.runtime.syntax.Context.Variable
import scala.language.dynamics

/**
 * A mutable, transactional context.
 *
 * @param txn Underlying transaction.
 */
class Context(var txn: Transaction) extends Dynamic {

  /**
   * Returns the value of the local variable with the specified name.
   *
   * @param name
   * @return
   */
  def selectDynamic(name: String): Variable = {
    Variable(name)
  }

  /**
   * Updates the value of the local variable with the specified name to the specified value.
   *
   * @param name
   * @param value
   * @param ctx
   */
  def updateDynamic(name: String)(value: Transaction)(implicit ctx: Context): Unit = {
    ctx += store(name, value)
  }

  /**
   * Appends the specified transaction to the underlying context.
   *
   * @param that Transaction to append.
   */
  def +=(that: Transaction): Unit = (this.txn, that) match {
    case (_, v: Literal) if v == Literal.Empty =>
    case (u: Literal, v) if u == Literal.Empty => this.txn = v
    case (u, v) => this.txn = cons(u, v)
  }

}

object Context {

  /**
   * An empty transactional context.
   *
   * @return Empty context.
   */
  def empty: Context = new Context(Literal.Empty)


  /**
   *
   * @param name
   */
  case class Variable(name: String)
}