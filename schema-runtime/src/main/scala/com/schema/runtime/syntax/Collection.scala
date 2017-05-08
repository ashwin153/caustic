package com.schema.runtime
package syntax

/**
 * A transactional collection.
 *
 * @param proxy
 */
case class Collection(proxy: Proxy) {

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
    ctx.found = Literal.False

    // Find the index of the value in the list.
    For (0, proxy.length) { i =>
      If (!ctx.found && equal(proxy(i), value)) {
        ctx.found = Literal.True
      }
    }

    // Add the value to the end of the set if it does not exist.
    If (!ctx.found) {
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
    ctx.found = Literal.False

    // Find the index of the value in the list.
    For (0, proxy.length) { i =>
      If (!ctx.found && equal(proxy(i), value)) {
        ctx.found = Literal.True
      }
    }

    // Shift all the values by one index to the left if it exists.
    If (ctx.found) {
      For (ctx.index, proxy.length) { i =>
        proxy(i) = proxy(i + 1)
      }
    }
  }

}