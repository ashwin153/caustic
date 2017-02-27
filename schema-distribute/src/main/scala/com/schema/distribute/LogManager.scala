package com.schema.distribute

import com.schema.objects.{Schema, Snapshot, _}
import com.schema.distribute
import com.schema.log.{Transaction, TransactionRejectedException}
import com.schema.objects.transactions._
import scala.concurrent.Future
import scala.concurrent.duration.{Deadline, FiniteDuration}

/**
 * An asynchronous, eventually consistent, manager that performs conflict resolution by serializing
 * transactions over a shared [[Log]]. Each manager must maintain its own copy of the [[Snapshot]].
 * These individual copies are continuously updated by successfully applied transactions in the log,
 * and are guaranteed to be consistent with each other after the specified expiry period. Therefore,
 * a strongly consistent manager is simply a special case in which the expiration period is zero.
 *
 * @param snapshot Underlying snapshot.
 * @param transactions Transaction log.
 * @param changes Change cursor.
 * @param expires Expiration period.
 */
class LogManager(
  snapshot: Snapshot,
  transactions: Log[Transaction],
  changes: Cursor[Change],
  expires: FiniteDuration
) extends Manager(snapshot) {

  // Version of the snapshot (equal to the current lsn).
  private[this] var version: Long = -1

  // Last time the snapshot was modified.
  private[this] var modified: Deadline = Deadline.now

  /**
   * Synchronously refreshes the underlying snapshot by reading all the latest change records from
   * the underlying shared log. Implementation also updates the version number and modified
   * timestamps to ensure eventual consistency of the underlying Snapshot.
   */
  private[this] def refresh(records: Seq[Record[Change]]): Unit = {
    this.modified = Deadline.now
    if (records.nonEmpty) this.version = records.last.lsn

    records.foreach(_.payload.mutations.foreach {
      case (k, Upsert(v)) => this.snapshot += k -> v
      case (k, Delete) => this.snapshot -= k
    })
  }

  override def commit(instructions: Map[String, Instruction]): Future[Unit] = {
    this.transactions.append(distribute.Transaction(this.version, instructions)) flatMap { t =>
      this.changes.advance(_.lsn >= t.lsn) flatMap { records =>
        // Refresh the snapshot with all records read from the log.
        refresh(records)

        // Check that the appended transaction was successfully applied.
        if (this.version == t.lsn)
          Future.unit
        else
          Future.failed(distribute.TransactionRejectedException("Transaction conflicts."))
      }
    } recover { case e => Failed(e) }
  }

  override def txn[T](f: (Schema) => Result[T]): Future[Outcome[T]] = {
    Future.unit flatMap { _ =>
      // If the snapshot is stale, then refresh it with all new entries.
      if ((this.modified + this.expires).isOverdue)
        changes.advance().map(refresh)
      else
        Future.unit
    } flatMap { _ => super.txn(f) }
  }

}

object LogManager {

  /**
   * Constructs a [[Manager]] for the specified snapshot that utilizes the specified transaction log
   * to perform transactional modifications to the snapshot. This manager is guaranteed to be
   * consistent after the expiry duration.
   *
   * @param snapshot Underlying snapshot.
   * @param log Transaction log.
   * @param expires Expiration period.
   * @return Transactional snapshot manager.
   */
  def apply(snapshot: Snapshot, log: Log[Transaction], expires: FiniteDuration): Manager = {
    // Stores the mapping of identifiers to version numbers.
    var versions = Map.empty[String, Long]

    // Construct a cursor over successfully applied transactions.
    val cursor = log.read(0) transform {
      case Record(lsn, txn) if !txn.conflicts(versions) =>
        txn.instructions.foreach(versions += _._1 -> lsn)
        Change(txn.instructions.toSeq collect { case (id, m: Mutation) => (id, m) })
    }

    // Construct a manager using the construct change stream.
    new LogManager(snapshot, log, cursor, expires)
  }

}