package caustic.syntax.scala
package visitors

import caustic.grammar.CausticBaseVisitor
import caustic.grammar.CausticParser._

/**
 *
 * @param symbols
 */
case class ExpressionVisitor(
  symbols: Map[String, Symbol]
) extends CausticBaseVisitor[String] {

  override def visitConstant(ctx: ConstantContext): String = {
    if (ctx.True() != null)
      s"""True"""
    else if (ctx.False() != null)
      s"""False"""
    else if (ctx.Number() != null)
      s"""real(${ctx.Number().getText.toDouble})"""
    else if (ctx.String() != null)
      s"""text(${ctx.String().getText})"""
    else
      visitChildren(ctx)
  }

  override def visitSymbol(ctx: SymbolContext): String = this.symbols(ctx.getText) match {
    case x: Variable => s"""load(add(load(text("__scope__"), text(${x.name})))"""
    case x: Reference => s"""read(text(${x.key}))"""
    case _ => super.visitChildren(ctx)
  }

  override def visitFuncall(ctx: FuncallContext): String = this.symbols(ctx.getText) match {
    case x: Function => x.result
    case _ => super.visitChildren(ctx)
  }

  override def visitPrimaryExpression (ctx: PrimaryExpressionContext): String =
    if (ctx.symbol() != null)
      visitSymbol(ctx.symbol())
    else if (ctx.funcall() != null)
      visitFuncall(ctx.funcall())
    else if (ctx.constant() != null)
      visitConstant(ctx.constant())
    else if (ctx.expression() != null)
      visitExpression(ctx.expression())
    else
      visitChildren(ctx)

  override def visitPrefixExpression(ctx: PrefixExpressionContext): String = {
    if (ctx.Add() != null)
      visitPrimaryExpression(ctx.primaryExpression())
    else if (ctx.Sub() != null)
      s"""sub(Zero, ${visitPrimaryExpression(ctx.primaryExpression())})"""
    else if (ctx.Not() != null)
      s"""not(${visitPrimaryExpression(ctx.primaryExpression())})"""
    else
      super.visitChildren(ctx)
  }

  override def visitMultiplicativeExpression(ctx: MultiplicativeExpressionContext): String = {
    // Fetch the left-hand and right-hand sides of the expression.
    val lhs = ctx.multiplicativeExpression()
    val rhs = ctx.prefixExpression()

    // Recurse on higher precedence expressions.
    if (lhs != null && ctx.Mul() != null)
      s"""mul(${visitMultiplicativeExpression(lhs)}, ${visitPrefixExpression(rhs)})"""
    else if (lhs != null && ctx.Div() != null)
      s"""div(${visitMultiplicativeExpression(lhs)}, ${visitPrefixExpression(rhs)})"""
    else if (lhs != null && ctx.Mod() != null)
      s"""mod(${visitMultiplicativeExpression(lhs)}, ${visitPrefixExpression(rhs)})"""
    else
      super.visitChildren(ctx)
  }

  override def visitAdditiveExpression(ctx: AdditiveExpressionContext): String = {
    // Fetch the left-hand and right-hand sides of the expression.
    val lhs = ctx.additiveExpression()
    val rhs = ctx.multiplicativeExpression()

    // Recurse on higher precedence expressions.
    if (lhs != null && ctx.Add() != null)
      s"""add(${visitAdditiveExpression(lhs)}, ${visitMultiplicativeExpression(rhs)})"""
    else if (lhs != null && ctx.Sub() != null)
      s"""sub(${visitAdditiveExpression(lhs)}, ${visitMultiplicativeExpression(rhs)})"""
    else
      super.visitChildren(ctx)
  }

  override def visitRelationalExpression(ctx: RelationalExpressionContext): String = {
    // Fetch the left-hand and right-hand sides of the expression.
    val lhs = ctx.relationalExpression()
    val rhs = ctx.additiveExpression()

    // Recurse on higher precedence expressions.
    if (lhs != null && ctx.LessThan() != null)
      s"""lt(${visitRelationalExpression(lhs)}, ${visitAdditiveExpression(rhs)})"""
    else if (lhs != null && ctx.LessEqual() != null)
      s"""le(${visitRelationalExpression(lhs)}, ${visitAdditiveExpression(rhs)})"""
    else if (lhs != null && ctx.GreaterThan() != null)
      s"""gt(${visitRelationalExpression(lhs)}, ${visitAdditiveExpression(rhs)})"""
    else if (lhs != null && ctx.GreaterEqual() != null)
      s"""ge(${visitRelationalExpression(lhs)}, ${visitAdditiveExpression(rhs)})"""
    else
      super.visitChildren(ctx)
  }

  override def visitEqualityExpression(ctx: EqualityExpressionContext): String = {
    // Fetch the left-hand and right-hand sides of the expression.
    val lhs = ctx.equalityExpression()
    val rhs = ctx.relationalExpression()

    // Recurse on higher precedence expressions.
    if (lhs != null && ctx.Equal() != null)
      s"""equal(${visitEqualityExpression(lhs)}, ${visitRelationalExpression(rhs)})"""
    else if (lhs != null && ctx.NotEqual() != null)
      s"""negate(equal(${visitEqualityExpression(lhs)}, ${visitRelationalExpression(rhs)}))"""
    else
      super.visitChildren(ctx)
  }

  override def visitLogicalAndExpression(ctx: LogicalAndExpressionContext): String = {
    // Fetch the left-hand and right-hand sides of the expression.
    val lhs = ctx.logicalAndExpression()
    val rhs = ctx.equalityExpression()

    // Recurse on higher precedence expressions.
    if (lhs != null)
      s"""both(${visitLogicalAndExpression(lhs)}, ${visitEqualityExpression(rhs)})"""
    else
      super.visitChildren(ctx)
  }

  override def visitLogicalOrExpression(ctx: LogicalOrExpressionContext): String = {
    // Fetch the left-hand and right-hand sides of the expression.
    val lhs = ctx.logicalOrExpression()
    val rhs = ctx.logicalAndExpression()

    // Recurse on higher precedence expressions.
    if (lhs != null)
      s"""either(${visitLogicalOrExpression(lhs)}, ${visitLogicalAndExpression(rhs)})"""
    else
      super.visitChildren(ctx)
  }

  override def visitExpression(ctx: ExpressionContext): String =
    if (ctx.logicalOrExpression() != null)
      visitLogicalOrExpression(ctx.logicalOrExpression())
    else
      super.visitExpression(ctx)

}