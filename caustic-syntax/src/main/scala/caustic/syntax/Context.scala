package caustic.syntax

import scala.language.dynamics

/**
 *
 * @param txn Transaction builder.
 */
class Context(private[syntax] var txn: Transaction) extends Dynamic {

  /**
   *
   * @param name Variable name.
   * @return
   */
  def get(name: Transaction): Transaction =
    load(name)

  def selectDynamic(name: String): Transaction =
    get(name)

  /**
   *
   * @param name Variable name.
   * @param value Updated value.
   */
  def set(name: Transaction, value: Transaction): Unit =
    this += store(name, value)

  def updateDynamic(name: String)(value: Transaction): Unit =
    set(name, value)

  /**
   *
   * @param that Transaction to append.
   */
  private[syntax] def +=(that: Transaction): Unit =
    this.txn = cons(this.txn, that)

}

object Context {

  /**
   * Constructs an empty transaction context.
   *
   * @return Empty Context.
   */
  def empty: Context = new Context(text(""))

}
