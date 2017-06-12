package caustic.distribute

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * An asynchronous, uni-directional log cursor. Cursors are not thread-safe.
 *
 * @tparam T Type of cursor entries.
 */
trait Cursor[T] { self =>

  def next()(
    implicit ec: ExecutionContext
  ): Future[Entry[T]]

  /**
   * Advances the cursor until an entry that satisfies the specified predicate is found. Returns a
   * future containing all the records up to and including the record that satisfies the predicate,
   * which completes when an entry that satisfies the predicate is found.
   *
   * @param p Predicate function.
   * @return Future containing all records up to the entry that satisfies the predicate.
   */
  def advance(p: Entry[T] => Boolean)(
    implicit ec: ExecutionContext
  ): Future[Seq[Record[T]]] =
    next().flatMap {
      case e: Record[T]  => if (p(e)) Future(Seq(e)) else advance(p).map(_.+:(e))
      case e: Pending[T] => if (p(e)) Future(Seq.empty) else advance(p)
    }

  /**
   * Advances the cursor until the specified number of records are found. Returns a future
   * containing a sequence of records, which completes when all n records are found.
   *
   * @param n Number of records to advance.
   * @return Future containing all n records.
   */
  def advance(n: Long)(
    implicit ec: ExecutionContext
  ): Future[Seq[Record[T]]] =
    if (n <= 0)
      Future(Seq.empty)
    else
      next().flatMap {
        case e: Record[T] => advance(n - 1).map(_.+:(e))
        case _: Pending[T] => advance(n)
      }

  /**
   * Advances the cursor until a pending entry is found. Returns a future containing a sequence of
   * records, which completes when the first pending entry is found.
   *
   * @return Future containing all records until the first pending entry.
   */
  def advance()(
    implicit ec: ExecutionContext
  ): Future[Seq[Record[T]]] =
    advance {
      e => e match {
        case _: Pending[T] => true
        case _ => false
      }
    }


  /**
   * Applies the specified function to each record until the first pending entry is found. Returns
   * a future which completes when the function has been successfully applied to each record.
   *
   * @param f Function to apply to each record.
   * @tparam U Type of function result.
   * @return Future that completes when the function has been applied to all records.
   */
  def foreach[U](f: Record[T] => U)(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    advance().map(_.foreach(f))

  /**
   * Returns a cursor whose records are transformed by the specified partial function, discarding
   * records over which the partial function is not defined. Advancement of the transformed cursor
   * will advance the underlying cursor, and vice versa. Therefore, once a cursor has been
   * transformed it is unsafe to use it anymore.
   *
   * @param f Transformation function.
   * @tparam U Type of transformation result.
   * @return Cursor containing transformed records.
   */
  def transform[U](f: PartialFunction[Record[T], U])(
    implicit ec: ExecutionContext
  ): Cursor[U] = new Cursor[U] {
    override def next()(implicit ec: ExecutionContext): Future[Entry[U]] =
      self.next().flatMap {
        case e: Pending[T] => Future(Pending(e.offset))
        case e: Record[T]  => Future(f(e)).transformWith {
          case Success(p)  => Future(Record(e.offset, p))
          case Failure(_)  => next()
        }
      }
  }

  /**
   * Returns a cursor whose records are transformed by the specified mapping. Advancement of the
   * mapped cursor will advance the underlying cursor, and vice versa. Therefore, once a cursor has
   * been mapped it is unsafe to use it anymore.
   *
   * @param f Mapping function.
   * @tparam U Type of mapped cursor.
   * @return Cursor containing mapped records.
   */
  def map[U](f: Record[T] => U)(
    implicit ec: ExecutionContext
  ): Cursor[U] =
    transform { case t => f(t) }

  /**
   * Returns a cursor whose records are filtered by the specified predicate. Records that do not
   * satisfy the specified predicate are ignored. Advancement of the filtered cursor will advance
   * the underlying cursor, and vice versa. Therefore, once a cursor has been filtered it is unsafe
   * to use it anymore.
   *
   * @param f Filtration function.
   * @return Cursor containing filtered records.
   */
  def filter(f: Record[T] => Boolean)(
    implicit ec: ExecutionContext
  ): Cursor[T] =
    transform { case t if f(t) => t.payload }

}
