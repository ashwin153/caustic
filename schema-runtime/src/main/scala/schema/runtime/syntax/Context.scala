package schema.runtime
package syntax

import Context._
import scala.language.dynamics

/**
 * A mutable, transactional context.
 *
 * @param txn Underlying transaction.
 */
final class Context(var txn: Transaction) extends Dynamic {

  /**
   *
   * @param name
   * @return
   */
  def selectDynamic(name: String): Variable =
    Variable(name)

  /**
   *
   * @param name
   * @param value
   * @param ctx
   */
  def updateDynamic(name: String)(value: Transaction)(
    implicit ctx: Context
  ): Unit =
    ctx += store(name, value)

  /**
   *
   * @param that
   */
  def +=(that: Transaction): Unit = (this.txn, that) match {
    case (_, v: Literal) if v == Literal.Empty =>
    case (u: Literal, v) if u == Literal.Empty => this.txn = v
    case (u, v) => this.txn = cons(u, v)
  }

}

object Context {

  /**
   * A local variable.
   *
   * @param name Variable name.
   */
  final case class Variable(name: String)

  /**
   * Constructs an empty transaction context.
   *
   * @return Empty context.
   */
  def empty: Context = new Context(Literal.Empty)

}