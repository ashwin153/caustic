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
   * Returns the local variable with the specified name.
   *
   * @param name Variable name.
   * @return Corresponding variable.
   */
  def selectDynamic(name: String): Variable =
    Variable(name)

  /**
   * Updates the variable with the specified name to the specified value.
   *
   * @param name Variable name.
   * @param value New value.
   * @param ctx Implicit transaction context.
   */
  def updateDynamic(name: String)(value: Transaction)(
    implicit ctx: Context
  ): Unit =
    ctx += store(name, value)

  /**
   * Appends the specified transaction to the underlying state.
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