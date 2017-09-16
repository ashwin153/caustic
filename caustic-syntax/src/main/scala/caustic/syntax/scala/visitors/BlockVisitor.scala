package caustic.syntax.scala
package visitors

import caustic.grammar.CausticBaseVisitor
import caustic.grammar.CausticParser._

/**
 *
 * @param namespace
 * @param symbols
 */
case class BlockVisitor(
  symbols: Map[String, Symbol]
) extends CausticBaseVisitor[Transaction] {

  override def visitBlock(ctx: BlockContext): Transaction =
    super.visitBlock(ctx)

  override def visitStatement(ctx: StatementContext): Transaction =
    super.visitStatement(ctx)

  override def visitRollback(ctx: RollbackContext): Transaction =
    super.visitRollback(ctx)

  override def visitDefinition(ctx: DefinitionContext): Transaction =
    super.visitDefinition(ctx)

  override def visitAssignment(ctx: AssignmentContext): String = {
    // Update the left-hand side with the right-hand side.
    this.symbols(ctx.symbol()) match {
      case Variable(name, _) => s"store(add(load(text("__scope__"), text($name)), $rhs)"
      case Reference(key, _) => s"write(text($key), $rhs)"
    }


    // Determine the right-hand side of the assignment operation.
    val rhs = ctx match {
      case _ if ctx.Assign() != null =>
        visitResult(ctx.result())
      case _ if ctx.AddAssign() != null =>
        add(this.expression.visitSymbol(ctx.symbol()), visitResult(ctx.result()))
      case _ if ctx.SubAssign() != null =>
        sub(this.expression.visitSymbol(ctx.symbol()), rhs)
      case _ if ctx.MulAssign() != null =>
        mul(this.expression.visitSymbol(ctx.symbol()), rhs)
      case _ if ctx.DivAssign() != null =>
        div(this.expression.visitSymbol(ctx.symbol()), rhs)
      case _ if ctx.ModAssign() != null =>
        mod(this.expression.visitSymbol(ctx.symbol()), rhs)
    }

    // Determine the left-hand and right-hand side of the assignment.
    val lhs = visitSymbol(ctx.symbol())


    super.visitChildren(ctx)
  }

  override def visitValue(ctx: ValueContext): Transaction = {
    if (ctx.expression() != null)
      ExpressionVisitor(this.symbols).visitExpression(ctx.expression())
    else if (ctx.symbol() != null)
      this.symbols(ctx.getText) match {
        case x: Variable => s"""add(load(text("__scope__"), text(${x.name}))"""
        case x: Reference => s"""read(text(${x.key}))"""
        case _ => super.visitChildren(ctx)
      }
    if (ctx.symbol() != null)
      this.expression.visitSymbol(ctx.symbol())
    else if (ctx.expression() != null)
      this.expression.visitExpression(ctx.expression())
    else
      super.visitChildren(ctx)
  }

  override def visitDeletion(ctx: DeletionContext): Transaction =
    super.visitDeletion(ctx)

  override def visitLoop(ctx: LoopContext): Transaction =
    super.visitLoop(ctx)

  override def visitConditional(ctx: ConditionalContext): Transaction =
    super.visitChildren(ctx)

}