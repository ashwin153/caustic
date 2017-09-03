package caustic.syntax.core
package collections

/**
 *
 */
case class Entry(name: Transaction, parent: Record) extends Assignable with Evaluable with Removable {

  override def get: Transaction = read(parent.key.)

  override def set(value: Transaction): Transaction = ???

  override def delete(recursive: Boolean): Transaction = ???
}
