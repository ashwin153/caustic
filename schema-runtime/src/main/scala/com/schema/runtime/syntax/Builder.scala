package com.schema.runtime.syntax

import com.schema.runtime.Transaction.Literal
import com.schema.runtime.{Transaction, cons}

/**
 * A transaction builder.
 *
 * @param txn Underlying transaction.
 */
case class Builder(var txn: Transaction) {

  /**
   *
   * @param that
   */
  def :+(that: Transaction): Unit = this.txn match {
    case l: Literal if l == Literal.Empty => this.txn = that
    case _ => this.txn = cons(this.txn, that)
  }

}
