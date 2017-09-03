package caustic.syntax.core.collections

import caustic.runtime.thrift.Transaction
import caustic.syntax.core.Record

case class Map(owner: Record) {

  def get(key: Transaction): Transaction = read(this.owner.key + "@" + key)

  def set(key: Transaction, value: Transaction) = ???

}
