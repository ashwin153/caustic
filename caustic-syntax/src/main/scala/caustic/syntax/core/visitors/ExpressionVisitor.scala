package caustic.syntax.core
package visitors

import caustic.grammar.CausticBaseVisitor
import caustic.grammar.CausticParser._

/**
 *
 * @param symbols
 */
case class ExpressionVisitor(
  symbols: Map[String, Symbol]
) extends CausticBaseVisitor[Transaction] {

  override def visitLogicalOrExpression(ctx: LogicalOrExpressionContext): Transaction = {
    // Fetch the left-hand and right-hand sides of the expression.
    val lhs = ctx.logicalOrExpression()
    val rhs = ctx.logicalAndExpression()

    // Recurse on higher precedence expressions.
    if (lhs != null)
      or(visitLogicalOrExpression(lhs), visitLogicalAndExpression(rhs))
    else
      super.visitChildren(ctx)
  }

  override def visitLogicalAndExpression(ctx: LogicalAndExpressionContext): Transaction = {
    // Fetch the left-hand and right-hand sides of the expression.
    val lhs = ctx.logicalAndExpression()
    val rhs = ctx.equalityExpression()

    // Recurse on higher precedence expressions.
    if (lhs != null)
      or(visitLogicalAndExpression(lhs), visitEqualityExpression(rhs))
    else
      super.visitChildren(ctx)
  }

  override def visitEqualityExpression(ctx: EqualityExpressionContext): Transaction = {
    // Fetch the left-hand and right-hand sides of the expression.
    val lhs = ctx.equalityExpression()
    val rhs = ctx.relationalExpression()

    // Recurse on higher precedence expressions.
    if (lhs != null && ctx.Equal() != null)
      equal(visitEqualityExpression(lhs), visitRelationalExpression(rhs))
    else if (lhs != null && ctx.NotEqual() != null)
      not(equal(visitEqualityExpression(lhs), visitRelationalExpression(rhs)))
    else
      super.visitChildren(ctx)
  }

  override def visitRelationalExpression(ctx: RelationalExpressionContext): Transaction = {
    // Fetch the left-hand and right-hand sides of the expression.
    val lhs = ctx.relationalExpression()
    val rhs = ctx.additiveExpression()

    // Recurse on higher precedence expressions.
    if (lhs != null && ctx.LessThan() != null)
      lt(visitRelationalExpression(lhs), visitAdditiveExpression(rhs))
    else if (lhs != null && ctx.LessEqual() != null)
      le(visitRelationalExpression(lhs), visitAdditiveExpression(rhs))
    else if (lhs != null && ctx.GreaterThan() != null)
      gt(visitRelationalExpression(lhs), visitAdditiveExpression(rhs))
    else if (lhs != null && ctx.GreaterEqual() != null)
      ge(visitRelationalExpression(lhs), visitAdditiveExpression(rhs))
    else
      super.visitChildren(ctx)
  }

  override def visitAdditiveExpression(ctx: AdditiveExpressionContext): Transaction = {
    // Fetch the left-hand and right-hand sides of the expression.
    val lhs = ctx.additiveExpression()
    val rhs = ctx.multiplicativeExpression()

    // Recurse on higher precedence expressions.
    if (lhs != null && ctx.Add() != null)
      add(visitAdditiveExpression(lhs), visitMultiplicativeExpression(rhs))
    else if (lhs != null && ctx.Sub() != null)
      sub(visitAdditiveExpression(lhs), visitMultiplicativeExpression(rhs))
    else
      super.visitChildren(ctx)
  }

  override def visitMultiplicativeExpression(ctx: MultiplicativeExpressionContext): Transaction = {
    // Fetch the left-hand and right-hand sides of the expression.
    val lhs = ctx.multiplicativeExpression()
    val rhs = ctx.prefixExpression()

    // Recurse on higher precedence expressions.
    if (lhs != null && ctx.Mul() != null)
      add(visitMultiplicativeExpression(lhs), visitPrefixExpression(rhs))
    else if (lhs != null && ctx.Div() != null)
      div(visitMultiplicativeExpression(lhs), visitPrefixExpression(rhs))
    else if (lhs != null && ctx.Mod() != null)
      mod(visitMultiplicativeExpression(lhs), visitPrefixExpression(rhs))
    else
      super.visitChildren(ctx)
  }

  override def visitPrefixExpression(ctx: PrefixExpressionContext): Transaction = {
    if (ctx.Add() != null)
      visitPrimaryExpression(ctx.primaryExpression())
    else if (ctx.Sub() != null)
      sub(real(0), visitPrimaryExpression(ctx.primaryExpression()))
    else if (ctx.Not() != null)
      not(visitPrimaryExpression(ctx.primaryExpression()))
    else
      super.visitChildren(ctx)
  }

  override def visitPrimaryExpression (ctx: PrimaryExpressionContext): Transaction =
    if (ctx.symbol() != null)
      visitSymbol(ctx.symbol())
    else if (ctx.funcall() != null)
      Empty
    else if (ctx.constant() != null)
      visitConstant(ctx.constant())
    else if (ctx.expression() != null)
      visitExpression(ctx.expression())
    else
      visitChildren(ctx)

  override def visitFuncall(ctx: FuncallContext): Transaction =
    super.visitFuncall(ctx)

  override def visitSymbol(ctx: SymbolContext): Transaction =
    //

    this.symbols(ctx.Identifier(0).getText) match {
      case Variable(n, s) =>
      case Function(r, a) =>
      case Record(p) => p.find(_.name === )
    }
    // Find symbol corresponding to the name.
    this.symbols(ctx.Identifier(0).getText) match {
      case v: Variable => load(v.name)
      case r: Reference => read(r.key)
      case r: Record =>
    }

  override def visitConstant(ctx: ConstantContext): Transaction = {
    if (ctx.True() != null)
      flag(true)
    else if (ctx.False() != null)
      flag(false)
    else if (ctx.Number() != null)
      real(ctx.Number().getText.toDouble)
    else if (ctx.String() != null)
      text(ctx.String().getText)
    else
      visitChildren(ctx)
  }

  "x" -> Function("Foo", "x", "y" , "bar")
  "Foo" -> Record()
  "y" -> Variable()


}

