package caustic.syntax

import scala.language.dynamics


/**
 *
 * @param txn
 */
class Context(private[syntax] var txn: Transaction) extends Dynamic {

  /**
   *
   * @param name
   * @return
   */
  def get(name: Transaction): Transaction =
    load(name)

  def selectDynamic(name: String): Transaction =
    get(name)

  /**
   *
   * @param name
   * @param value
   */
  def set(name: Transaction, value: Transaction): Unit =
    this += store(name, value)

  def updateDynamic(name: String)(value: Transaction): Unit =
    set(name, value)

  /**
   *
   * @param that
   */
  private[syntax] def +=(that: Transaction): Unit =
    this.txn = cons(this.txn, that)

}

object Context {

  /**
   *
   * @return
   */
  def empty: Context = new Context(text(""))

}
