package com.schema.core.transactions

import Manager._
import com.schema.core.{Schema, _}
import java.util.concurrent.{RejectedExecutionException, ScheduledExecutorService}
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Deadline, FiniteDuration}
import scala.concurrent.{Future, Promise}
import scala.util.control.NonFatal

/**
 * An asynchronous transactional manager. Enables transactional modifications to be performed on an
 * arbitrary snapshot. Utilizes [[Schema]] to ensure static type safety.
 *
 * @param snapshot Underlying snapshot.
 */
abstract class Manager(snapshot: Snapshot) {

  def commit(instructions: Map[String, Instruction]): Future[Unit]

  /**
   * Asynchronously attempts to perform the transaction specified by the provided builder function.
   * Returns a future containing the outcome of the transaction or an exception.
   *
   * @param f Transaction builder function.
   * @return Future containing the outcome of the transaction.
   */
  def txn[T](f: Schema => Result[T]): Future[Outcome[T]] = {
    // Construct an empty schema and apply the builder function.
    val instructions = mutable.Map.empty[String, Instruction]
    val schema = new Schema(TransactionalSnapshot(this.snapshot, instructions))

    // Depending on the result of builder function, either commit or rollback.
    f(schema) match {
      case Rollback(value) => Future(Rollbacked(value))
      case Commit(value) => commit(instructions.toMap)
        .map(_ => Committed(value))
        .recover { case e => Failed(e) }
    }
  }

  /**
   * Asynchronously attempts to perform the transaction specified by the provided builder function
   * until it produces an outcome of [[Committed]], [[Rollbacked]], or all retries are exhausted.
   * Retries are implemented as a finite sequence of finite backoff durations. Therefore, each
   * transaction is guaranteed to eventually complete.
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

  /**
   * A snapshot that collects modifications. Transactional snapshots are guaranteed to (1) never
   * directly modify the underlying snapshot and (2) always return the latest value for any key.
   * Therefore, if any particular key is modified, then the new value will always be returned
   * whenever it is accessed even though the underlying snapshot has not been changed.
   *
   * @param instructions Instructions to be performed on the snapshot.
   */
  case class TransactionalSnapshot(
    underlying: Snapshot,
    instructions: mutable.Map[String, Instruction]
  ) extends Snapshot {

    override def get(key: String): Option[Any] = {
      this.instructions.get(key) match {
        case Some(Upsert(v)) => Some(v)
        case Some(Delete) => None
        case _ => underlying.get(key) map { v => this.instructions += key -> Read; v }
      }
    }

    override def +=(kv: (String, Any)): Unit =
      this.instructions += kv._1 -> Upsert(kv._2)

    override def -=(key: String): Unit = this.instructions -= key

  }

}