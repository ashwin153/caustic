package caustic.syntax.objects

import caustic.runtime.thrift

/**
 * A sequence of keys, in which each key is prefixed by its length and keys are stored in ascending
 * order. Similar in functionality to the Redis ZipList.
 */
case class Sequence(underlying: thrift.Transaction) {

  /**
   *
   * @param key
   * @return
   */
  def (key: thrift.Transaction): thrift.Transaction = {

  }

}
