package com.schema.runtime
package syntax

/**
 * A transactional collection.
 *
 * @param proxy
 */
case class Index(proxy: Proxy) {

  /**
   *
   * @return
   */
  def length: Transaction =
    this.proxy.length

  /**
   *
   * @param index
   */
  def apply(index: Transaction): Proxy =
    this.proxy(index)

  /**
   *
   * @param value
   * @param ctx
   */
  def +=(value: Transaction)(implicit ctx: Context): Unit = {
    // Setup local variables.
    ctx.found = -1

    // Find the index of the value in the list.
    For (ctx.i, 0, proxy.length) {
      If (proxy(ctx.i) == value) {
        ctx.found = ctx.i
      }
    }

    // Add the value to the end of the set if it does not exist.
    If (ctx.found < 0) {
      proxy(proxy.length) = value
      proxy.length = proxy.length + 1
    }
  }

  /**
   *
   * @param value
   * @param ctx
   */
  def -=(value: Transaction)(implicit ctx: Context): Unit = {
    // Setup local variables.
    ctx.found = -1

    // Find the index of the value in the list.
    For (ctx.i, 0, proxy.length) {
      If (proxy(ctx.i) == value) {
        ctx.found = ctx.i
      }
    }

    // Shift all the values by one index to the left if it exists.
    If (ctx.found >= 0) {
      For (ctx.i, ctx.found, proxy.length) {
        proxy(ctx.i) = proxy(ctx.i + 1)
      }
    }
  }

  /**
   *
   * @param ctx
   */
  def clear()(implicit ctx: Context): Unit = {
    For (ctx.i, 0, proxy.length) {
      proxy(ctx.i) = Literal.Empty
    }

    proxy.length = 0
  }

}