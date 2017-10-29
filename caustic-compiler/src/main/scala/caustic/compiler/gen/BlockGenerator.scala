package caustic.compiler.gen

import caustic.grammar._
import caustic.compiler.typing._
import scala.collection.JavaConverters._

/**
 *
 * @param universe
 */
case class BlockGenerator(
  universe: Universe
) extends CausticBaseVisitor[String] {

  override def visitBlock(ctx: CausticParser.BlockContext): String = {
    ctx.statement().asScala.map(visitStatement).reduce((a, b) => s"""cons($a, $b)""")
  }

  override def visitStatement(ctx: CausticParser.StatementContext): String = {
    visitChildren(ctx)
  }

  override def visitRollback(ctx: CausticParser.RollbackContext): String = {
    val message = ExpressionGenerator(this.universe).visitExpression(ctx.expression())
    s"""rollback(${ message.value })"""
  }

  override def visitDefinition(ctx: CausticParser.DefinitionContext): String = {
    // Determine the value of the variable.
    val rhs = ExpressionGenerator(this.universe).visitExpression(ctx.expression())
    this.universe.putVariable(ctx.Identifier().getText, rhs.tag)

    // Add the variable to the t table and update its value.
    val lhs = this.universe.getVariable(ctx.Identifier().getText)
    s"""store("${ lhs.name }", ${ rhs.value })"""
  }

  override def visitAssignment(ctx: CausticParser.AssignmentContext): String = {
    // Determine the value of the variable.
    var rhs = ExpressionGenerator(this.universe).visitExpression(ctx.expression())
    val cur = ExpressionGenerator(this.universe).visitName(ctx.name())

    if (ctx.AddAssign() != null)
      rhs = Result(lub(cur.tag, rhs.tag), s"""add(${ cur.value }, ${ rhs.value })""")
    else if (ctx.SubAssign() != null)
      rhs = Result(lub(cur.tag, rhs.tag), s"""sub(${ cur.value }, ${ rhs.value })""")
    else if (ctx.MulAssign() != null)
      rhs = Result(lub(cur.tag, rhs.tag), s"""mul(${ cur.value }, ${ rhs.value })""")
    else if (ctx.DivAssign() != null)
      rhs = Result(lub(cur.tag, rhs.tag), s"""div(${ cur.value }, ${ rhs.value })""")
    else if (ctx.ModAssign() != null)
      rhs = Result(lub(cur.tag, rhs.tag), s"""mod(${ cur.value }, ${ rhs.value })""")

    // Copy the value into the variable.
    val lhs = NameGenerator(this.universe).visitName(ctx.name())
    BlockGenerator.copy(lhs, rhs)
  }

  override def visitDeletion(ctx: CausticParser.DeletionContext): String = {
    BlockGenerator.delete(NameGenerator(this.universe).visitName(ctx.name()))
  }

  override def visitLoop(ctx: CausticParser.LoopContext): String = {
    // Load the loop condition and body.
    val condition = ExpressionGenerator(this.universe).visitExpression(ctx.expression())
    val block = BlockGenerator(this.universe.child()).visitBlock(ctx.block())

    // Serialize the loop as a repeat expression.
    s"""repeat(${ condition.value }, $block)"""
  }

  override def visitConditional(ctx: CausticParser.ConditionalContext): String = {
    // Construct the if/elif/else branches.
    val blocks = ctx.block().asScala.map(BlockGenerator(this.universe.child()).visitBlock)
    val compares = ctx.expression().asScala.map(ExpressionGenerator(this.universe).visitExpression)
    val branches = compares.zip(blocks).map { case (c, b) => s"""branch(${ c.value }, $b, """ }

    // Append the else block and closing parenthesis.
    val last = if (ctx.Else() != null) blocks.last else "None"
    branches.mkString + last + ")" * branches.size
  }

}

object BlockGenerator {

  /**
   *
   * @param base
   * @return
   */
  def fields(base: Result): Iterable[Result] = base.tag match {
    case Record(fields) =>
      val names = fields.keys.map(f => s"""add(${ base.value }, "@$f")""")
      val types = fields.values.map(t => if (t.isInstanceOf[Pointer]) Textual else t)
      types.zip(names).map { case (a, b) => Result(a, b) }
    case Pointer(Record(fields)) =>
      val names = fields.keys.map(f => s"""add(${ base.value }, "@$f")""")
      val types = fields.values.map(t => if (t.isInstanceOf[Pointer]) Pointer(Textual) else t)
      types.zip(names).map { case (a, b) => Result(a, b) }
  }

  /**
   *
   * @param lhs
   * @param rhs
   * @return
   */
  def copy(lhs: Result, rhs: Result): String = (lhs.tag, rhs.tag) match {
    case (_: Primitive, _: Primitive) =>
      // Copy primitive values.
      s"""store(${ lhs.value }, ${ rhs.value })"""
    case (_: Primitive, Pointer(_: Primitive)) =>
      // Dereference primitive pointers.
      s"""store(${ lhs.value }, read(${ rhs.value }))"""
    case (Pointer(_: Primitive), _: Primitive) =>
      // Copy primitive values.
      s"""write(${ lhs.value }, ${ rhs.value })"""
    case (Pointer(_: Primitive), Pointer(_: Primitive)) =>
      // Copy primitive pointers.
      s"""write(${ lhs.value }, ${ rhs.value })"""
    case _ =>
      // Verify the values correspond to the same record.
      (lhs.tag, rhs.tag) match {
        case (u: Record, v: Record) if u == v =>
        case (u: Record, Pointer(v: Record)) if u == v =>
        case (Pointer(u: Record), v: Record) if u == v =>
        case (Pointer(u: Record), Pointer(v: Record)) if u == v =>
      }

      // Recursively copy the fields of the record.
      fields(lhs).zip(fields(rhs))
        .map { case (a, b) => copy(a, b) }
        .foldLeft("None") { case (a: String, b: String) => s"cons($a, $b)" }
  }

  /**
   *
   * @param lhs
   * @return
   */
  def delete(lhs: Result): String = lhs.tag match {
    case _: Primitive =>
      s"""store(${ lhs.value }, None)"""
    case Pointer(_: Primitive) =>
      s"""write(${ lhs.value }, None)"""
    case _ =>
      // Recursively delete the fields of the record.
      fields(lhs).map(delete).foldLeft("None") { case (a: String, b: String) => s"cons($a, $b)" }
  }

}