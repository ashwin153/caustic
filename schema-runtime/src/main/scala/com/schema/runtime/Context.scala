package com.schema.runtime

import com.schema.runtime.Transaction.Literal

/**
 * A mutable, transaction builder.
 *
 * @param txn Underlying transaction.
 */
case class Context(var txn: Transaction) {

  /**
   * Appends the specified transaction using a cons operation.
   *
   * @param that Transaction to append.
   */
  def :+(that: Transaction): Unit = (this.txn, that) match {
    case (_, v: Literal) if v == Literal.Empty =>
    case (u: Literal, v) if u == Literal.Empty => this.txn = v
    case (u, v) => this.txn = cons(u, v)
  }

}
