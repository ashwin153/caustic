package com.schema.runtime
package syntax

import scala.language.dynamics

/**
 * A mutable, transactional context.
 *
 * @param txn Underlying transaction.
 */
class Context(
  private[syntax] var txn: Transaction
) extends Dynamic {

  /**
   * Appends the specified transaction to the underlying context.
   *
   * @param that Transaction to append.
   */
  private[syntax] def +=(that: Transaction): Unit = (this.txn, that) match {
    case (_, v: Literal) if v == Literal.Empty =>
    case (u: Literal, v) if u == Literal.Empty => this.txn = v
    case (u, v) => this.txn = cons(u, v)
  }

  /**
   * Returns the value of the local variable with the specified name.
   *
   * @param name
   * @return
   */
  private[syntax] def selectDynamic(name: String): Transaction =
    load(name)

  /**
   * Updates the value of the local variable with the specified name to the specified value.
   *
   * @param name
   * @param value
   * @param ctx
   */
  private[syntax] def updateDynamic(name: String)(value: Transaction)(implicit ctx: Context): Unit =
    ctx += store(name, value)

}

object Context {

  /**
   *
   * @return
   */
  def empty: Context = new Context(Literal.Empty)

}