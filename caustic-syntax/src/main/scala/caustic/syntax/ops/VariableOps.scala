package caustic.syntax
package ops

import caustic.runtime.thrift.Transaction

/**
 *
 * @param x
 */
case class VariableOps(x: Variable) {

  def +=(y: Transaction)(implicit ctx: Context): Unit = ctx.append(store(x.name, x + y))
  def -=(y: Transaction)(implicit ctx: Context): Unit = ctx.append(store(x.name, x - y))
  def *=(y: Transaction)(implicit ctx: Context): Unit = ctx.append(store(x.name, x * y))
  def /=(y: Transaction)(implicit ctx: Context): Unit = ctx.append(store(x.name, x / y))
  def %=(y: Transaction)(implicit ctx: Context): Unit = ctx.append(store(x.name, x % y))
  def ++=(y: Transaction)(implicit ctx: Context): Unit = ctx.append(store(x.name, x ++ y))

}
