package caustic.syntax

import Object._

/**
 *
 * @param keys
 */
case class Index(keys: Transaction) {

  /**
   *
   * @param f
   * @return
   */
  def filter(f: Transaction => Transaction)(
    implicit ctx: Context
  ): Index = {
    ctx.$i = 0
    ctx.$keys = ""

    While (ctx.$i < length(this.keys)) {
      ctx.$j = this.keys.indexOf(ArrayDelimiter, ctx.$i)
      val key = this.keys.substring(ctx.$i, ctx.$j)
      If (f(key)) {
        ctx.$keys ++= key ++ ArrayDelimiter
      }
      ctx.$i = ctx.$j + 1
    }

    Index(ctx.$keys)
  }

  /**
   *
   * @param f
   * @return
   */
  def map(f: Transaction => Transaction)(
    implicit ctx: Context
  ): Index = {
    ctx.$i = 0
    ctx.$keys = ""

    While (ctx.$i < length(this.keys)) {
      ctx.$j = this.keys.indexOf(ArrayDelimiter, ctx.$i)
      val key = this.keys.substring(ctx.$i, ctx.$j)
      ctx.$keys ++= f(key) ++ ArrayDelimiter
      ctx.$i = ctx.$j + 1
    }

    Index(ctx.$keys)
  }

  /**
   *
   * @param f
   * @param g
   * @param ctx
   * @return
   */
  def foldLeft(f: Transaction)(g: (Transaction, Object) => Transaction)(
    implicit ctx: Context
  ): Transaction = {
    ctx += prefetch(this.keys)
    ctx.$result = f
    ctx.$i = 0

    While (ctx.$i < length(this.keys)) {
      ctx.$j = this.keys.indexOf(ArrayDelimiter, ctx.$i)
      val key = this.keys.substring(ctx.$i, ctx.$j)
      ctx.$result = g(ctx.$result, Object(key, None))
      ctx.$i = ctx.$j + 1
    }

    ctx.$result
  }

  Seq.empty.foldLeft()
  /**
   *
   * @param f
   * @return
   */
  def foreach(f: Object => Unit)(
    implicit ctx: Context
  ): Unit = {
    ctx += prefetch(this.keys)
    ctx.$result = true
    ctx.$i = 0

    While (ctx.$i < length(this.keys)) {
      ctx.$j = this.keys.indexOf(ArrayDelimiter, ctx.$i)
      val key = this.keys.substring(ctx.$i, ctx.$j)
      f(Object(key, None))
      ctx.$i = ctx.$j + 1
    }
  }

  /**
   *
   * @param f
   * @param ctx
   * @return
   */
  def forall(f: Object => Transaction)(
    implicit ctx: Context
  ): Transaction = {
    ctx += prefetch(this.keys)
    ctx.$result = true
    ctx.$i = 0

    While (ctx.$i < length(this.keys) && ctx.$result) {
      ctx.$j = this.keys.indexOf(ArrayDelimiter, ctx.$i)
      val key = this.keys.substring(ctx.$i, ctx.$j)
      ctx.$result = f(Object(key, None))
      ctx.$i = ctx.$j + 1
    }

    ctx.$result
  }

}