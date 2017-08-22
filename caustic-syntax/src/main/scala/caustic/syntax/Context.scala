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
  def get(name: String): Variable =
    Variable(name)

  def selectDynamic(name: String): Variable =
    get(name)

  /**
   *
   * @param name
   * @param value
   */
  def set(name: String, value: Transaction): Unit =
    append(store(name, value))

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
