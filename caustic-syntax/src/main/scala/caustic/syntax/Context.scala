package caustic.syntax

import Context._
import syntax._
import caustic.runtime.thrift

import scala.language.dynamics

/**
 * A mutable, transactional context.
 *
 * @param txn Underlying transaction.
 */
case class Context(
  private[syntax] var txn: thrift.Transaction
) extends Dynamic {

  /**
   * Returns the local variable with the specified name.
   *
   * @param name Variable name.
   * @return Corresponding variable.
   */
  def selectDynamic(name: String): Variable = Variable(name)

  /**
   * Updates the variable with the specified name to the specified value.
   *
   * @param name Variable name.
   * @param value New value.
   */
  def updateDynamic(name: String)(value: thrift.Transaction): Unit =
    this.append(store(name, value))

  /**
   * Appends the specified transaction to the underlying state.
   *
   * @param that Transaction to append.
   */
  def append(that: thrift.Transaction): Unit =
    this.txn = cons(this.txn, that)

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
