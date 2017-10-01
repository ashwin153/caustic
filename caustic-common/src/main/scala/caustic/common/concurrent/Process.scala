package caustic.common.concurrent

import caustic.common.concurrent.Process._

import shapeless._
import shapeless.ops.hlist._

import scala.concurrent._

/**
 * An asynchronous, type-safe task.
 */
trait Process[T <: HList] {

  /**
   * Begins the process, and returns a future that completes on termination. Subsequent calls to
   * this method should always return the same future.
   *
   * @return Future of process outputs.
   */
  def start(): Future[T]

  /**
   * Signals that the process should terminate.
   */
  def stop(): Unit

  /**
   * Returns a process which executes this process and then that process.
   *
   * @param that Subsequent process.
   * @param ec Implicit execution context.
   * @param prepend Implicit HList prepend.
   * @return Sequential processes.
   */
  def before[U <: HList, O <: HList](that: Process[U])(
    implicit ec: ExecutionContext,
    prepend: Prepend.Aux[T, U, O]
  ): Process[O] = sequential(this, that)

  /**
   * Returns a process which executes this process and that process simultaneously.
   *
   * @param that Parallel process.
   * @param ec Implicit execution context.
   * @param prepend Implicit HList prepend.
   * @return Concurrent processes.
   */
  def during[U <: HList, O <: HList](that: Process[U])(
    implicit ec: ExecutionContext,
    prepend: Prepend.Aux[T, U, O]
  ): Process[O] = concurrent(this, that)

  /**
   * Returns a process which executes that process and then this process.
   *
   * @param that Preceding process.
   * @param ec Implicit execution context.
   * @param prepend Implicit HList prepend.
   * @return Sequential processes.
   */
  def after[U <: HList, O <: HList](that: Process[U])(
    implicit ec: ExecutionContext,
    prepend: Prepend.Aux[U, T, O]
  ): Process[O] = sequential(that, this)

}

object Process {

  /**
   * Constructs a cancellable process from the specified asynchronous task.
   *
   * @param task Asynchronous task.
   * @param cancel Optional cancellation.
   * @param ec Implicit execution context.
   * @return Cancellable process.
   */
  def async[T](task: => Future[T], cancel: => Unit = Unit)(
    implicit ec: ExecutionContext
  ): Process[T :: HNil] = new Process[T :: HNil] {
    override lazy val start: Future[T :: HNil] = task.map(_ :: HNil)
    override def stop(): Unit = cancel
  }

  /**
   * Constructs a cancellable process from the specified synchronous task.
   *
   * @param task Synchronous task.
   * @param cancel Optional cancellation.
   * @param ec Implicit execution context.
   * @return Cancellable process.
   */
  def sync[T](task: => T, cancel: => Unit = Unit)(
    implicit ec: ExecutionContext
  ): Process[T :: HNil] = async(Future(blocking(task)), cancel)

  /**
   * Constructs a process that blocks indefinitely until it is stopped.
   *
   * @param ec Implicit execution context.
   * @return Indefinite process.
   */
  def infinite(
    implicit ec: ExecutionContext
  ): Process[HNil] = new Process[HNil] {
    private val promise = Promise[HNil]
    override lazy val start: Future[HNil] = this.promise.future
    override def stop(): Unit = this.promise.success(HNil)
  }

  /**
   * Constructs a process that executes the specified subprocesses in parallel.
   *
   * @param x Subprocess.
   * @param y Subprocess.
   * @param ec Implicit execution context.
   * @param prepend Implicit HList prepend.
   * @return Parallel process.
   */
  def concurrent[T <: HList, U <: HList, O <: HList](x: Process[T], y: Process[U])(
    implicit ec: ExecutionContext,
    prepend: Prepend.Aux[T, U, O]
  ): Process[O] = new Process[O] {

    override lazy val start: Future[O] = {
      // Start both futures simultaneously.
      val xf = x.start()
      val yf = y.start()

      // Combine the results of the future into a HList.
      xf.flatMap(a => yf.map(b => a ::: b))
    }

    override def stop(): Unit = {
      // Stop the futures sequentially.
      x.stop()
      y.stop()
    }

  }

  /**
   * Constructs a process that executes the specified subprocesses sequentially.
   *
   * @param x First subprocess.
   * @param y Second subprocess.
   * @param ec Implicit execution context.
   * @param prepend Implicit HList prepend.
   * @return Chained processes.
   */
  def sequential[T <: HList, U <: HList, O <: HList](x: Process[T], y: Process[U])(
    implicit ec: ExecutionContext,
    prepend: Prepend.Aux[T, U, O]
  ): Process[O] = new Process[O] {

    override lazy val start: Future[O] = {
      // Start the first future, and Start the second when the first completes.
      x.start().flatMap(a => y.start().map(b => a ::: b))
    }

    override def stop(): Unit = {
      // Stop the futures in reverse order.
      y.stop()
      x.stop()
    }

  }

}
