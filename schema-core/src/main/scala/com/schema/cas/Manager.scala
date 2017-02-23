package com.schema.cas

import com.schema.core.{Schema, Snapshot}
import com.schema.cas.Manager._
import com.schema.log.{Cursor, Log, Record}
import java.util.concurrent.{RejectedExecutionException, ScheduledExecutorService}
import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.{Deadline, FiniteDuration}
import scala.util.control.NonFatal

/**
 * An asynchronous, eventually consistent, transactional snapshot manager. Managers guarantee that
 * the snapshot will be consistent after the specified expiry period. Therefore, a strongly
 * consistent manager is constructed with an expiration period of zero.
 *
 * @param underlying Underlying snapshot.
 * @param transactions Transaction log.
 * @param changes Change cursor.
 * @param expires Expiration period.
 */
class Manager private (
  underlying: Snapshot,
  transactions: Log[Transaction],
  changes: Cursor[Change],
  expires: FiniteDuration
) {

  // Version of the snapshot (equal to the current lsn).
  private[this] var version: Long = -1

  // Last time the snapshot was modified.
  private[this] var modified: Deadline = Deadline.now

  /**
   * A transactionally modifiable snapshot. Transactional snapshots record the intent to perform
   * instructions on key-value pairs in the underlying snapshot. Transactional snapshots are
   * guaranteed to (1) never directly modify the underlying snapshot and (2) always return the
   * latest value for any key; therefore, if a particular key is modified, then the updated value
   * of the key will be returned on the next get request.
   *
   * @param instructions Instructions to be performed on the snapshot.
   */
  case class TransactionalSnapshot(
    instructions: mutable.Map[String, Instruction]
  ) extends Snapshot {

    override def get(key: String): Option[Any] = {
      this.instructions.get(key) match {
        case Some(Upsert(v)) => Some(v)
        case Some(Delete) => None
        case _ => underlying.get(key) map { v => this.instructions += key -> Read; v }
      }
    }

    override def +=(kv: (String, Any)): Unit = this.instructions += kv._1 -> Upsert(kv._2)

    override def -=(key: String): Unit = this.instructions -= key

  }

  /**
   * Synchronously refreshes the underlying snapshot with the specified change records.
   * Implementation updates the version number to the latest record's lsn and sets the modified
   * timestamp to the current time.
   */
  private[this] def refresh(records: Seq[Record[Change]]): Unit = {
    this.modified = Deadline.now
    if (records.nonEmpty) this.version = records.last.lsn

    records.foreach(_.payload.mutations.foreach {
      case (k, Upsert(v)) => this.underlying += k -> v
      case (k, Delete) => this.underlying -= k
    })
  }

  /**
   * Asynchronously attempts to perform the transaction specified by the provided builder function.
   * Implementation first refreshes the underlying snapshot if it has expired to ensure that the
   * snapshot is up-to-date. Returns a future containing the outcome of the transaction or an
   * exception if the transaction could not be successfully applied.
   *
   * @param f Transaction builder function.
   * @return Future containing the outcome of the transaction.
   */
  def txn[T](f: Schema => Result[T]): Future[Outcome[T]] =
    Future.unit flatMap { _ =>
      // If the snapshot is stale, then refresh it with all new entries.
      if ((this.modified + this.expires).isOverdue)
        changes.advance().map(refresh)
      else
        Future.unit
    } flatMap { _ =>
      // Construct an empty schema and apply the builder function.
      val snapshot = TransactionalSnapshot(mutable.Map.empty)
      val schema = new Schema(snapshot)
      val transaction = Transaction(this.version, snapshot.instructions.toMap)

      // Depending on the result of builder function, either commit or rollback.
      f(schema) match {
        case Rollback(value) => Future(Rollbacked(value))
        case Commit(value) => this.transactions.append(transaction) flatMap { t =>
          this.changes.advance(_.lsn >= t.lsn) flatMap { records =>
            // Refresh the snapshot with all records read from the log.
            refresh(records)

            // If the current snapshot version matches the lsn of the appended transaction, then
            // we know that the transaction was successfully applied, otherwise return failure.
            if (this.version == t.lsn)
              Future(Committed(value))
            else
              Future(Failed(TransactionRejectedException("Transaction conflicts, no more retries.")))
          }
        }
      }
    }

  /**
   * A retryable transaction. Retries are implemented as a sequence of finite backoff durations.
   * Therefore, every transaction is guaranteed to terminate at some point in time.
   *
   * @param backoffs Sequence of finite backoff durations.
   * @param f Transaction builder function.
   * @param scheduler Implicit scheduler.
   * @tparam T Type of transaction result.
   * @return Future containing the outcome of the transaction.
   */
  def txn[T](backoffs: Seq[FiniteDuration])(f: Schema => Result[T])(
    implicit scheduler: ScheduledExecutorService
  ): Future[Outcome[T]] =
    txn(f) flatMap {
      case Failed(NonFatal(_)) => after(backoffs.head, txn(backoffs.drop(1))(f))
      case x @ _ => Future(x)
    }


}

object Manager {

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
    new Manager(snapshot, log, cursor, expires)
  }

  /**
   * Schedules the specified function to be run on the implicitly specified scheduled executor after
   * the specified delay. Returns a future that contains the result of the function, which completes
   * when the function has been performed.
   *
   * @param delay Duration to delay execution.
   * @param run Function to execute.
   * @param scheduler Implicit scheduler.
   * @tparam T Type of result.
   * @return Future containing the result of executing the function.
   * @throws RejectedExecutionException if the task cannot be scheduled for execution
   */
  def after[T](delay: FiniteDuration, run: => Future[T])(
    implicit scheduler: ScheduledExecutorService
  ): Future[T] = {
    val promise = Promise[T]()
    scheduler.schedule(() => promise.completeWith(run), delay.length, delay.unit)
    promise.future
  }

}