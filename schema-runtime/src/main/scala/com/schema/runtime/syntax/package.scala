package com.schema.runtime

import Transaction._
import akka.actor.Scheduler
import akka.pattern.after
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

package object syntax {

  implicit def obj2txn(obj: Object): Transaction = obj.key match {
    case l: Literal => read(l)
    case o: Operation => o
  }

  implicit def obj2richtxn(obj: Object): RichTransaction = obj2txn(obj)

  /**
   * Utilizes an implicit Akka Scheduler in order to schedule retries with the provided backoff
   * durations. Common backoff strategy implements can be found in Backoff.scala in the finagle-core
   * project.
   *
   * @param backoffs
   * @param f
   * @tparam T
   */
  def Retry[T](backoffs: Stream[FiniteDuration])(f: => Future[T])(
    implicit ec: ExecutionContext,
    scheduler: Scheduler
  ): Future[T] =
    f.recoverWith { case _ if backoffs.nonEmpty =>
      after(backoffs.head, scheduler)(Retry(backoffs.drop(1))(f))
    }

  /**
   *
   * @param f
   * @param db
   * @param ec
   * @return
   */
  def Transaction(f: Builder => Unit)(
    implicit ec: ExecutionContext,
    db: Database
  ): Future[String] = {
    // Build the transaction.
    val builder = Builder(Literal.Empty)
    f(builder)

    // Execute the transaction.
    db.execute(builder.txn)
  }

  /**
   *
   * @param key
   * @return
   */
  def Select(key: Key)(
    implicit builder: Builder
  ): Object = {
    require(!key.contains(FieldDelimiter), "Key may not contain field delimiter.")
    require(!key.contains(ListDelimiter), "Key may not contain list delimiter.")
    builder :+ read(key)
    Object(Literal(key))
  }

  /**
   *
   * @param obj
   * @param builder
   */
  def Delete(obj: Object)(
    implicit builder: Builder
  ): Unit = {
    If (obj.key.contains(FieldDelimiter)) {
      builder :+ write(obj.key, "")
    } Else {
      builder :+ purge(read(obj.key))
      builder :+ write(obj.key, "")
    }
  }

  /**
   *
   * Appends a conditional branch into the transaction context and returns a structural type
   * that enables an optional Else clause to be subsequently specified. Implementation requires that
   * the language feature scala.language.reflectiveCalls to be in scope in order to silence any
   * compiler warnings.
   *
   * @param cmp Comparison condition.
   * @param success Operations to perform if the condition is true.
   * @param builder Implicit transaction context.
   * @return Optional else clause object.
   */
  def If(cmp: Transaction)(success: => Unit)(
    implicit builder: Builder
  ) = new {
    private val before = builder.txn
    builder.txn = Literal.Empty
    success
    private val pass = builder.txn
    builder.txn = cons(before, branch(cmp, pass, Literal.Empty))

    def Else(failure: => Unit): Unit = {
      builder.txn = Literal.Empty
      failure
      val fail = builder.txn
      builder.txn = cons(before, branch(cmp, pass, fail))
    }
  }

  /**
   *
   * @param results
   * @param builder
   */
  def Return(results: Transaction*)(
    implicit builder: Builder
  ): Unit =
    if (results.size == 1)
      builder :+ results.head
    else
      builder :+ concat("[", concat(results.reduceLeft((a, b) => concat(concat(a, ","), b)), "]"))

}
