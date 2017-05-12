package com.schema.runtime
package syntax

import com.schema.runtime.syntax.Context.Variable
import scala.language.dynamics

/**
 * A mutable, transactional context.
 *
 * @param txn Underlying transaction.
 */
final class Context(var txn: Transaction) extends Dynamic {

  def selectDynamic(name: String): Variable =
    Variable(name)

  def updateDynamic(name: String)(value: Transaction)(implicit ctx: Context): Unit =
    ctx += store(name, value)

  def +=(that: Transaction): Unit = (this.txn, that) match {
    case (_, v: Literal) if v == Literal.Empty =>
    case (u: Literal, v) if u == Literal.Empty => this.txn = v
    case (u, v) => this.txn = cons(u, v)
  }

}

object Context {

  /**
   *
   * @param name
   */
  final case class Variable(name: String)

  /**
   *
   * @return
   */
  def empty: Context = new Context(Literal.Empty)

}