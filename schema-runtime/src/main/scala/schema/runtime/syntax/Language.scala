package schema.runtime
package syntax

import Database._
import Context._
import akka.actor.ActorSystem
import akka.pattern.after
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

/**
 *
 */
trait Language {

  /**
   *
   * @param f
   * @param ec
   * @param db
   * @return
   */
  def Schema(backoffs: Stream[FiniteDuration])(f: Context => Unit)(
    implicit ec: ExecutionContext,
    akka: ActorSystem,
    db: Database
  ): Future[String] =
    Schema(f).recoverWith {
      case e: ExecutionException =>
        Future.failed(e)
      case NonFatal(_) if backoffs.nonEmpty =>
        after(backoffs.head, akka.scheduler)(Schema(backoffs.drop(1))(f))
    }

  /**
   *
   * @param f
   * @param ec
   * @param db
   * @return
   */
  def Schema(f: Context => Unit)(
    implicit ec: ExecutionContext,
    db: Database
  ): Future[String] = {
    val ctx = Context.empty
    f(ctx)
    db.execute(ctx.txn)
  }

  /**
   *
   * @param key
   * @param ctx
   * @return
   */
  def Select(key: Key)(
    implicit ctx: Context
  ): Object = {
    require(key.nonEmpty, "Key must be non-empty.")
    require(!key.contains(FieldDelimiter.value), s"Key may not contain ${FieldDelimiter.value}")
    require(!key.contains(ArrayDelimiter.value), s"Key may not contain ${ArrayDelimiter.value}")
    Object(key)
  }

  /**
   *
   * @param obj
   * @param ctx
   */
  def Delete(obj: Object)(
    implicit ctx: Context
  ): Unit = {
    // When working with loops, it is important to prefetch keys whenever possible.
    ctx += prefetch(obj.$fields)
    ctx += prefetch(obj.$indices)

    // Serialize the various fields of the object.
    If(length(obj.$fields) > 0) {
      ctx.$i = 0

      While(ctx.$i < length(obj.$fields)) {
        ctx.$j = ctx.$i
        While(!equal(obj.$fields.charAt(ctx.$j), ArrayDelimiter)) {
          ctx.$j = ctx.$j + 1
        }

        val name = obj.$fields.substring(ctx.$i, ctx.$j)
        ctx += write(obj.key ++ FieldDelimiter ++ name, Literal.Empty)
        ctx.$i = ctx.$j + 1
      }
    }

    // Serialize the various indices of the object.
    If (length(obj.$indices) > 0) {
      ctx.$i = 0

      While (ctx.$i < length(obj.$indices)) {
        ctx.$j = ctx.$i + 1
        While (!equal(obj.$indices.charAt(ctx.$j), ArrayDelimiter)) {
          ctx.$j = ctx.$j + 1
        }

        val name = obj.$indices.substring(ctx.$i, ctx.$j)
        val field = obj.key ++ FieldDelimiter ++ name
        val index = read(field ++ FieldDelimiter ++ literal("$values"))
        prefetch(index)
        ctx.$k = 0

        While(ctx.$k < length(index)) {
          ctx.$l = ctx.$k + 1
          While(!equal(index.charAt(ctx.$l), ArrayDelimiter)) {
            ctx.$l = ctx.$l + 1
          }

          val at = index.substring(ctx.$k, ctx.$l)
          ctx += write(field ++ FieldDelimiter ++ at, Literal.Empty)
          ctx.$k = ctx.$l + 1
        }

        ctx += write(field ++ FieldDelimiter ++ literal("$values"), Literal.Empty)
        ctx.$i = ctx.$j + 1
      }
    }

    // Clean up all our hidden variables and remove the existence marker on the object.
    obj.$fields = ""
    obj.$indices = ""
    ctx += write(obj.key, Literal.Empty)
  }

  /**
   *
   * @param cmp
   * @param success
   * @param ctx
   * @return
   */
  def If(cmp: Transaction)(success: => Unit)(
    implicit ctx: Context
  ) = new {
    private val before = ctx.txn
    ctx.txn = Literal.Empty
    success
    private val pass = ctx.txn
    ctx.txn = before
    ctx += branch(cmp, pass, Literal.Empty)

    def Else(failure: => Unit): Unit = {
      ctx.txn = Literal.Empty
      failure
      val fail = ctx.txn
      ctx.txn = before
      ctx += branch(cmp, pass, fail)
    }
  }

  /**
   *
   * @param cmp
   * @param block
   * @param ctx
   */
  def While(cmp: Transaction)(block: => Unit)(
    implicit ctx: Context
  ): Unit = {
    val before = ctx.txn
    ctx.txn = Literal.Empty
    block
    val body = ctx.txn
    ctx.txn = before
    ctx += repeat(cmp, body)
  }

  /**
   *
   * @param index
   * @param from
   * @param until
   * @param step
   * @param block
   * @param ctx
   */
  def For(
    index: Variable,
    from: Transaction,
    until: Transaction,
    step: Transaction = Literal.One
  )(
    block: => Unit
  )(
    implicit ctx: Context
  ): Unit = {
    ctx += store(index.name, from)
    While (load(index.name) < until) {
      block
      ctx += store(index.name, load(index.name) + step)
    }
  }

  /**
   *
   * @param index
   * @param in
   * @param block
   * @param ctx
   */
  def Foreach(index: Variable, in: Field)(block: => Unit)(
    implicit ctx: Context
  ): Unit = {
    // Prefetch all the values of the collection.
    val values = read(in.key ++ FieldDelimiter ++ "$values")
    ctx += prefetch(values)

    ctx.$i = 0
    While (ctx.$i < length(values)) {
      ctx.$j = ctx.$i + 1
      While (values.charAt(ctx.$j) <> ArrayDelimiter) {
        ctx.$j = ctx.$j + 1
      }

      // Load the index into the index variable and perform the block.
      val at = values.substring(ctx.$i, ctx.$j)
      ctx += store(index.name, in.key ++ FieldDelimiter ++ at)
      block
      ctx.$i = ctx.$j + 1
    }
  }

  /**
   *
   * @param index
   * @param in
   * @param block
   */
  def Foreach(index: Variable, in: Iterable[Key])(block: => Unit)(
    implicit ctx: Context
  ): Unit = {
    val before = ctx.txn
    ctx.txn = Literal.Empty
    block
    val body = ctx.txn
    ctx.txn = before
    ctx += prefetch(in.mkString(ArrayDelimiter))
    ctx += (0 until in.size).foldLeft[Transaction](Literal.Empty)((a, _) => cons(a, body))
  }

  /**
   *
   * @param obj
   */
  def Json(obj: Object): Transaction = {
    // When working with loops, it is important to prefetch keys whenever possible.
    implicit val ctx = Context.empty
    ctx += prefetch(obj.$fields)
    ctx += prefetch(obj.$indices)
    ctx.$json = literal("{\"key\":\"") ++ obj.key ++ "\""

    // Serialize the various fields of the object.
    If(length(obj.$fields) > 0) {
      ctx.$i = 0

      While(ctx.$i < length(obj.$fields)) {
        ctx.$j = ctx.$i
        While(!equal(obj.$fields.charAt(ctx.$j), ArrayDelimiter)) {
          ctx.$j = ctx.$j + 1
        }

        val name = obj.$fields.substring(ctx.$i, ctx.$j)
        val value = read(obj.key ++ FieldDelimiter ++ name)
        ctx.$json = ctx.$json ++ ",\"" ++ name ++ "\":\"" ++ value ++ "\""
        ctx.$i = ctx.$j + 1
      }
    }

    // Serialize the various indices of the object.
    If (length(obj.$indices) > 0) {
      ctx.$i = 0

      While (ctx.$i < length(obj.$indices)) {
        ctx.$j = ctx.$i + 1
        While (!equal(obj.$indices.charAt(ctx.$j), ArrayDelimiter)) {
          ctx.$j = ctx.$j + 1
        }

        val name = obj.$indices.substring(ctx.$i, ctx.$j)
        val index = read(obj.key ++ FieldDelimiter ++ name ++ FieldDelimiter ++ "$values")
        prefetch(index)
        ctx.$json = ctx.$json ++ ",\"" ++ name ++ "\":["
        ctx.$k = 0

        While(ctx.$k < length(index)) {
          ctx.$l = ctx.$k + 1
          While(!equal(index.charAt(ctx.$l), ArrayDelimiter)) {
            ctx.$l = ctx.$l + 1
          }

          val at = index.substring(ctx.$k, ctx.$l)
          val value = read(obj.key ++ FieldDelimiter ++ name ++ FieldDelimiter ++ at)
          ctx.$json = ctx.$json ++ "\"" ++ at ++ "\":\"" ++ value ++ "\","
          ctx.$k = ctx.$l + 1
        }

        ctx.$json = ctx.$json.substring(0, length(ctx.$json) - 1) ++ "]"
        ctx.$i = ctx.$j + 1
      }
    }

    // Place the serialized value into the context.
    ctx.$json = ctx.$json ++ "}"
    ctx += ctx.$json
    ctx.txn
  }

  /**
   *
   * @param first
   * @param rest
   * @param ctx
   */
  def Return(first: Transaction, rest: Transaction*)(
    implicit ctx: Context
  ): Unit =
    if (rest.isEmpty)
      ctx += first
    else
      ctx += concat("[", concat(
        rest.+:(first)
          .map(t => concat("\"", concat(t, "\"")))
          .reduceLeft((a, b) => a ++ "," ++ b),
        "]"
      ))

  /**
   *
   * @param message
   * @param ctx
   */
  def Rollback(message: Transaction = Literal.Empty)(
    implicit ctx: Context
  ): Unit =
    ctx += rollback(message)

  /**
   *
   * @param ctx
   */
  def Abort(
    implicit ctx: Context
  ): Unit =
    ctx += abort()

}
