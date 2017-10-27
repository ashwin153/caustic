package caustic.compiler.generate

import caustic.grammar._
import caustic.compiler.typing._

import scala.collection.JavaConverters._

/**
 *
 * @param universe
 */
case class ExpressionGenerator(
  universe: Universe
) extends CausticBaseVisitor[Result] {

  override def visitExpression(ctx: CausticParser.ExpressionContext): Result = {
    visitLogicalOrExpression(ctx.logicalOrExpression())
  }

  override def visitLogicalOrExpression(ctx: CausticParser.LogicalOrExpressionContext): Result = {
    val lhs = ctx.logicalOrExpression()
    val rhs = visitLogicalAndExpression(ctx.logicalAndExpression())

    if (lhs != null)
      Result(Primitive, s"""either(${ visitLogicalOrExpression(lhs).value }, ${ rhs.value })""")
    else
      rhs
  }

  override def visitLogicalAndExpression(ctx: CausticParser.LogicalAndExpressionContext): Result = {
    val lhs = ctx.logicalAndExpression()
    val rhs = visitEqualityExpression(ctx.equalityExpression())

    if (lhs != null)
      Result(Primitive, s"""both(${ visitLogicalAndExpression(lhs).value }, ${ rhs.value })""")
    else
      rhs
  }

  override def visitEqualityExpression(ctx: CausticParser.EqualityExpressionContext): Result = {
    val lhs = ctx.equalityExpression()
    val rhs = visitRelationalExpression(ctx.relationalExpression())

    if (lhs != null && ctx.Equal() != null)
      Result(Primitive, s"""equal(${ visitEqualityExpression(lhs).value }, ${ rhs.value })""")
    else if (lhs != null && ctx.NotEqual() != null)
      Result(Primitive, s"""notEqual(${ visitEqualityExpression(lhs).value }, ${ rhs.value }) })""")
    else
      rhs
  }

  override def visitRelationalExpression(ctx: CausticParser.RelationalExpressionContext): Result = {
    val lhs = ctx.relationalExpression()
    val rhs = visitAdditiveExpression(ctx.additiveExpression())

    if (lhs != null && ctx.LessThan() != null)
      Result(Primitive, s"""less(${ visitRelationalExpression(lhs).value }, ${ rhs.value })""")
    else if (lhs != null && ctx.LessEqual() != null)
      Result(Primitive, s"""lessEqual(${ visitRelationalExpression(lhs).value }, ${ rhs.value })""")
    else if (lhs != null && ctx.GreaterThan() != null)
      Result(Primitive, s"""greater(${ visitRelationalExpression(lhs).value }, ${ rhs.value })""")
    else if (lhs != null && ctx.GreaterEqual() != null)
      Result(Primitive, s"""greaterEqual(${ visitRelationalExpression(lhs).value }, ${ rhs.value })""")
    else
      rhs
  }

  override def visitAdditiveExpression(ctx: CausticParser.AdditiveExpressionContext): Result = {
    val lhs = ctx.additiveExpression()
    val rhs = visitMultiplicativeExpression(ctx.multiplicativeExpression())

    if (lhs != null && ctx.Add() != null)
      Result(Primitive, s"""add(${ visitAdditiveExpression(lhs).value }, ${ rhs.value })""")
    else if (lhs != null && ctx.Sub() != null)
      Result(Primitive, s"""sub(${ visitAdditiveExpression(lhs).value }, ${ rhs.value })""")
    else
      rhs
  }

  override def visitMultiplicativeExpression(ctx: CausticParser.MultiplicativeExpressionContext): Result = {
    val lhs = ctx.multiplicativeExpression()
    val rhs = visitPrefixExpression(ctx.prefixExpression())

    if (lhs != null && ctx.Mul() != null)
      Result(Primitive, s"""mul(${ visitMultiplicativeExpression(lhs).value }, ${ rhs.value })""")
    else if (lhs != null && ctx.Div() != null)
      Result(Primitive, s"""div(${ visitMultiplicativeExpression(lhs).value }, ${ rhs.value })""")
    else if (lhs != null && ctx.Mod() != null)
      Result(Primitive, s"""mod(${ visitMultiplicativeExpression(lhs).value }, ${ rhs.value })""")
    else
      rhs
  }

  override def visitPrefixExpression(ctx: CausticParser.PrefixExpressionContext): Result = {
    val rhs = visitPrimaryExpression(ctx.primaryExpression())

    if (ctx.Add() != null)
      rhs
    else if (ctx.Sub() != null)
      Result(Primitive, s"""sub(Zero, ${ rhs.value })""")
    else if (ctx.Not() != null)
      Result(Primitive, s"""negate(${ rhs.value })""")
    else
      visitChildren(ctx)
  }

  override def visitPrimaryExpression(ctx: CausticParser.PrimaryExpressionContext): Result =
    if (ctx.name() != null)
      visitName(ctx.name())
    else if (ctx.funcall() != null)
      visitFuncall(ctx.funcall())
    else if (ctx.constant() != null)
      visitConstant(ctx.constant())
    else if (ctx.expression() != null)
      visitExpression(ctx.expression())
    else
      visitChildren(ctx)

  override def visitName(ctx: CausticParser.NameContext): Result =
    NameGenerator(this.universe).visitName(ctx) match {
      case Result(Primitive, v) =>
        // Automatically load primitives.
        Result(Primitive, s"""load($v)""")
      case Result(Pointer(Primitive), v) =>
        // Automatically read primitive pointers.
        Result(Primitive, s"""read($v)""")
      case x =>
        // Otherwise, pass through values normally.
        x
    }

  override def visitFuncall(ctx: CausticParser.FuncallContext): Result = {
    val func = this.universe.getFunction(ctx.Identifier().getText)
    require(func.args.size == ctx.expression().size(), "Insufficient arguments")

    // Set the value of the arguments and copy the function body.
    Result(func.returns.tag, func.args.zip(ctx.expression().asScala.map(visitExpression))
      .map { case ((n, x), Result(y, v)) if x == y => s"""store($n, $v)""" }
      .foldRight(func.returns.value)((a, b) => s"""cons($a, $b)"""))
  }

  override def visitConstant(ctx: CausticParser.ConstantContext): Result = {
    if (ctx.True() != null)
      Result(Primitive, s"""flag(true)""")
    else if (ctx.False() != null)
      Result(Primitive, s"""flag(false)""")
    else if (ctx.Number() != null)
      Result(Primitive, s"""real(${ ctx.Number().getText.toDouble })""")
    else if (ctx.String() != null)
      Result(Primitive, s"""text(${ ctx.String().getText })""")
    else
      visitChildren(ctx)
  }

}