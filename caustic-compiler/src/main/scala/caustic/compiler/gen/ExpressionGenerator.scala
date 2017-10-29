package caustic.compiler.gen

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

  override def visitLogicalOrExpression(ctx: CausticParser.LogicalOrExpressionContext): Result =
    Option(ctx.logicalOrExpression()) match {
      case Some(x) =>
        // Evaluate bitwise or.
        val lhs = visitLogicalOrExpression(x)
        val rhs = visitLogicalAndExpression(ctx.logicalAndExpression())
        Result(lub(lhs.tag, rhs.tag), s"""either(${ lhs.value }, ${ rhs.value })""")
      case None =>
        // Recurse on higher precedence expressions.
        visitLogicalAndExpression(ctx.logicalAndExpression())
    }

  override def visitLogicalAndExpression(ctx: CausticParser.LogicalAndExpressionContext): Result =
    Option(ctx.logicalAndExpression()) match {
      case Some(x) =>
        // Evaluate bitwise and.
        val lhs = visitLogicalAndExpression(x)
        val rhs = visitEqualityExpression(ctx.equalityExpression())
        Result(lub(lhs.tag, rhs.tag), s"""both(${ lhs.value }, ${ rhs.value })""")
      case None =>
        // Recurse on higher precedence expressions.
        visitEqualityExpression(ctx.equalityExpression())
    }

  override def visitEqualityExpression(ctx: CausticParser.EqualityExpressionContext): Result =
    Option(ctx.equalityExpression()) match {
      case Some(x) =>
        // Evaluate equality.
        val lhs = visitEqualityExpression(x)
        val rhs = visitRelationalExpression(ctx.relationalExpression())

        if (ctx.Equal() != null)
          Result(lub(lhs.tag, rhs.tag), s"""equal(${ lhs.value }, ${ rhs.value })""")
        else if (ctx.NotEqual() != null)
          Result(lub(lhs.tag, rhs.tag), s"""notEqual(${ lhs.value }, ${ rhs.value }) })""")
        else
          visitChildren(ctx)
      case None =>
        // Recurse on higher precedence expressions.
        visitRelationalExpression(ctx.relationalExpression())
    }

  override def visitRelationalExpression(ctx: CausticParser.RelationalExpressionContext): Result =
    Option(ctx.relationalExpression()) match {
      case Some(x) =>
        // Evaluate comparisons.
        val lhs = visitRelationalExpression(x)
        val rhs = visitAdditiveExpression(ctx.additiveExpression())

        if (ctx.LessThan() != null)
          Result(lub(lhs.tag, rhs.tag), s"""less(${ lhs.value }, ${ rhs.value })""")
        else if (ctx.LessEqual() != null)
          Result(lub(lhs.tag, rhs.tag), s"""lessEqual(${ lhs.value }, ${ rhs.value })""")
        else if (ctx.GreaterThan() != null)
          Result(lub(lhs.tag, rhs.tag), s"""greater(${ lhs.value }, ${ rhs.value })""")
        else if (ctx.GreaterEqual() != null)
          Result(lub(lhs.tag, rhs.tag), s"""greaterEqual(${ lhs.value }, ${ rhs.value })""")
        else
        visitChildren(ctx)
      case None =>
        // Recurse on higher precedence expressions.
        visitAdditiveExpression(ctx.additiveExpression())
    }

  override def visitAdditiveExpression(ctx: CausticParser.AdditiveExpressionContext): Result =
    Option(ctx.additiveExpression()) match {
      case Some(x) =>
        // Evaluate addition and subtraction.
        val lhs = visitAdditiveExpression(x)
        val rhs = visitMultiplicativeExpression(ctx.multiplicativeExpression())

        if (ctx.Add() != null)
          Result(lub(lhs.tag, rhs.tag), s"""add(${ lhs.value }, ${ rhs.value })""")
        else if (ctx.Sub() != null)
          Result(lub(lhs.tag, rhs.tag), s"""sub(${ lhs.value }, ${ rhs.value })""")
        else
          visitChildren(ctx)
      case None =>
        // Recurse on higher precedence expressions.
        visitMultiplicativeExpression(ctx.multiplicativeExpression())
    }

  override def visitMultiplicativeExpression(ctx: CausticParser.MultiplicativeExpressionContext): Result =
    Option(ctx.multiplicativeExpression()) match {
      case Some(x) =>
        // Evaluate multiplication, division, and modulo.
        val lhs = visitMultiplicativeExpression(x)
        val rhs = visitPrefixExpression(ctx.prefixExpression())

        if (ctx.Mul() != null)
          Result(lub(lhs.tag, rhs.tag), s"""mul(${ lhs.value }, ${ rhs.value })""")
        else if (ctx.Div() != null && lhs.tag == Integer && rhs.tag == Integer)
          Result(lub(lhs.tag, rhs.tag), s"""floor(div(${ lhs.value }, ${ rhs.value }))""")
        else if (ctx.Div() != null)
          Result(lub(lhs.tag, rhs.tag), s"""div(${ lhs.value }, ${ rhs.value })""")
        else if (ctx.Mod() != null)
          Result(lub(lhs.tag, rhs.tag), s"""mod(${ lhs.value }, ${ rhs.value })""")
        else
          visitChildren(ctx)
      case None =>
        // Recurse on higher precedence expressions.
        visitPrefixExpression(ctx.prefixExpression())
    }

  override def visitPrefixExpression(ctx: CausticParser.PrefixExpressionContext): Result = {
    val rhs = visitPrimaryExpression(ctx.primaryExpression())
    if (ctx.Add() != null)
      rhs
    else if (ctx.Sub() != null)
      Result(rhs.tag, s"""sub(Zero, ${ rhs.value })""")
    else if (ctx.Not() != null)
      Result(rhs.tag, s"""negate(${ rhs.value })""")
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
      case Result(x: Primitive, v) =>
        // Automatically load primitives.
        Result(x, s"""load($v)""")
      case Result(Pointer(x: Primitive), v) =>
        // Automatically read primitive pointers.
        Result(x, s"""read($v)""")
      case x =>
        // Otherwise, pass through values normally.
        x
    }

  override def visitFuncall(ctx: CausticParser.FuncallContext): Result = {
    // Set the value of the arguments and return the function result.
    val func = this.universe.getFunction(ctx.Identifier().getText)
    Result(func.returns.tag, func.args.zip(ctx.expression().asScala.map(visitExpression))
      .map { case ((n, x), Result(y, v)) if x == y => s"""store($n, $v)""" }
      .foldRight(func.returns.value)((a, b) => s"""cons($a, $b)"""))
  }

  override def visitConstant(ctx: CausticParser.ConstantContext): Result = {
    if (ctx.True() != null)
      Result(Boolean, s"""flag(true)""")
    else if (ctx.False() != null)
      Result(Boolean, s"""flag(false)""")
    else if (ctx.Number() != null && ctx.Number().getText.contains('.'))
      Result(Decimal, s"""real(${ ctx.Number().getText.toDouble })""")
    else if (ctx.Number() != null)
      Result(Integer, s"""real(${ ctx.Number().getText.toDouble })""")
    else if (ctx.String() != null)
      Result(Textual, s"""text(${ ctx.String().getText })""")
    else
      visitChildren(ctx)
  }

}