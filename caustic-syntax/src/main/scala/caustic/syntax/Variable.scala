package caustic.syntax

/**
 *
 * @param name
 */
case class Variable(name: String) {

  /**
   *
   * @return
   */
  def get(): Transaction = load(name)

  /**
   *
   * @param value
   * @param ctx
   */
  def set(value: Transaction)(implicit ctx: Context): Unit = ctx.append(store(name, value))

}
