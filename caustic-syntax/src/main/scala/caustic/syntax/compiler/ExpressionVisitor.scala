package caustic.syntax
package compiler

import caustic.grammar._
import caustic.grammar.CausticParser._
import scala.collection.JavaConverters._

/**
 * An expression parser.
 *
 * @param symbols Symbol table.
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
      s"""text("${ctx.String().getText}")"""
    else
      visitChildren(ctx)
  }

  override def visitVariable(ctx: VariableContext): String = {
    // Determine field of the specified record and automatically dereference pointers.
    def field(parent: Type, names: List[String], key: String): String = (parent, names) match {
      case (Primitive, Nil) =>
        key
      case (Reference(Primitive), Nil) =>
        s"""read($key)"""
      case (Record(f), head :: rest) =>
        field(f(head), rest, s"""add($key, text("."), text("$head"))""")
      case (Reference(Record(f)), head :: rest) =>
        field(f(head), rest, s"""add(read($key), text("."), text("$head"))""")
    }

    // Load the corresponding variable.
    this.symbols(ctx.Identifier(0)) match {
      case Variable(name, _, Primitive) =>
        // Load the value of primitive variables.
        s"""load(text("$name"))"""
      case Variable(name, _, record: Record) =>
        // Load the corresponding field of the record.
        val names = ctx.Identifier().asScala.drop(1).map(_.getText).toList
        val key = field(record, names, s"""text("$name")""")
        s"""load($key)"""
      case Variable(name, _, Reference(Primitive)) =>
        // Automatically dereference primitive pointers.
        s"""read(load(text("$name")))"""
      case Variable(name, _, Reference(record: Record)) =>
        // Read the corresponding field of the record reference.
        val names = ctx.Identifier().asScala.drop(1).map(_.getText).toList
        val key = field(record, names, s"""load(text("$name"))""")
        s"""read($key)"""
    }
  }

  override def visitFuncall(ctx: FuncallContext): String =
    this.symbols(ctx.getText) match {
      case Function(args, _, body) =>
        // Store the value of the arguments, and insert the body of the function call.
        args.zip(ctx.expression().asScala.map(visitExpression))
          .map { case (x, v) => s"""store(text("${x.name}"), $v)""" }
          .foldRight(body)((a, b) => s"""cons($a, $b)""")
      case _ => visitChildren(ctx)
    }

  override def visitPrimaryExpression (ctx: PrimaryExpressionContext): String =
    if (ctx.variable() != null)
      visitVariable(ctx.variable())
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
      visitChildren(ctx)
  }

  override def visitMultiplicativeExpression(ctx: MultiplicativeExpressionContext): String = {
    val lhs = ctx.multiplicativeExpression()
    val rhs = ctx.prefixExpression()

    if (lhs != null && ctx.Mul() != null)
      s"""mul(${visitMultiplicativeExpression(lhs)}, ${visitPrefixExpression(rhs)})"""
    else if (lhs != null && ctx.Div() != null)
      s"""div(${visitMultiplicativeExpression(lhs)}, ${visitPrefixExpression(rhs)})"""
    else if (lhs != null && ctx.Mod() != null)
      s"""mod(${visitMultiplicativeExpression(lhs)}, ${visitPrefixExpression(rhs)})"""
    else
      visitChildren(ctx)
  }

  override def visitAdditiveExpression(ctx: AdditiveExpressionContext): String = {
    val lhs = ctx.additiveExpression()
    val rhs = ctx.multiplicativeExpression()

    if (lhs != null && ctx.Add() != null)
      s"""add(${visitAdditiveExpression(lhs)}, ${visitMultiplicativeExpression(rhs)})"""
    else if (lhs != null && ctx.Sub() != null)
      s"""sub(${visitAdditiveExpression(lhs)}, ${visitMultiplicativeExpression(rhs)})"""
    else
      visitChildren(ctx)
  }

  override def visitRelationalExpression(ctx: RelationalExpressionContext): String = {
    val lhs = ctx.relationalExpression()
    val rhs = ctx.additiveExpression()

    if (lhs != null && ctx.LessThan() != null)
      s"""lt(${visitRelationalExpression(lhs)}, ${visitAdditiveExpression(rhs)})"""
    else if (lhs != null && ctx.LessEqual() != null)
      s"""le(${visitRelationalExpression(lhs)}, ${visitAdditiveExpression(rhs)})"""
    else if (lhs != null && ctx.GreaterThan() != null)
      s"""gt(${visitRelationalExpression(lhs)}, ${visitAdditiveExpression(rhs)})"""
    else if (lhs != null && ctx.GreaterEqual() != null)
      s"""ge(${visitRelationalExpression(lhs)}, ${visitAdditiveExpression(rhs)})"""
    else
      visitChildren(ctx)
  }

  override def visitEqualityExpression(ctx: EqualityExpressionContext): String = {
    val lhs = ctx.equalityExpression()
    val rhs = ctx.relationalExpression()

    if (lhs != null && ctx.Equal() != null)
      s"""eq(${visitEqualityExpression(lhs)}, ${visitRelationalExpression(rhs)})"""
    else if (lhs != null && ctx.NotEqual() != null)
      s"""ne(${visitEqualityExpression(lhs)}, ${visitRelationalExpression(rhs)})"""
    else
      visitChildren(ctx)
  }

  override def visitLogicalAndExpression(ctx: LogicalAndExpressionContext): String = {
    val lhs = ctx.logicalAndExpression()
    val rhs = ctx.equalityExpression()

    if (lhs != null)
      s"""and(${visitLogicalAndExpression(lhs)}, ${visitEqualityExpression(rhs)})"""
    else
      visitChildren(ctx)
  }

  override def visitLogicalOrExpression(ctx: LogicalOrExpressionContext): String = {
    val lhs = ctx.logicalOrExpression()
    val rhs = ctx.logicalAndExpression()

    if (lhs != null)
      s"""or(${visitLogicalOrExpression(lhs)}, ${visitLogicalAndExpression(rhs)})"""
    else
      visitChildren(ctx)
  }

}