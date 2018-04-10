package caustic.compiler
package parsing

import caustic.compiler.typing._
import caustic.grammar._

import scala.collection.JavaConverters._
import scala.language.postfixOps

/**
 * An expression simplifier and type inferencer.
 *
 * @param universe Type universe.
 */
case class Simplify(universe: Universe) extends CausticBaseVisitor[Result] {

  override def visitBlock(ctx: CausticParser.BlockContext): Result = {
    val statements = ctx.statement().asScala.map(visitStatement)
    statements.reduceLeft((a, b) => b.copy(value = s"${a.value}\n${b.value}"))
  }

  override def visitStatement(ctx: CausticParser.StatementContext): Result =
    visitChildren(ctx)

  override def visitRollback(ctx: CausticParser.RollbackContext): Result = {
    val message = visitExpression(ctx.expression())
    Result(CUnit, s"Rollback (${ message.value })")
  }

  override def visitDefinition(ctx: CausticParser.DefinitionContext): Result = {
    val rhs = visitExpression(ctx.expression())
    val lhs = ctx.Identifier().getText

    val definition = rhs.kind match {
      case Pointer(k) => s"val $lhs = Reference[${ k.name }](Variable.Remote(${ rhs.value }))"
      case k: Primitive => s"val $lhs = Variable.Local[${ k.name }](context.label())"
      case k => s"val $lhs = Reference[${ k.name }](Variable.Local(context.label()))"

    }

    this.universe.bind(lhs, Variable(rhs.kind))
    Result(CUnit, s"$definition\n$lhs := ${ rhs.value }")
  }

  override def visitAssignment(ctx: CausticParser.AssignmentContext): Result = {
    var rhs = visitExpression(ctx.expression())
    val lhs = visitName(ctx.name())

    if (lub(lhs.kind, rhs.kind) != lhs.kind)
      throw Error.Type(s"Expected ${ lhs.kind.name }, but was ${ rhs.kind.name }", Error.Trace(ctx))
    else if (ctx.Assign() != null)
      Result(lhs.kind, s"${ lhs.value } := ${ rhs.value }")
    else if (ctx.AddAssign() != null)
      Result(lhs.kind, s"${ lhs.value } += ${ rhs.value }")
    else if (ctx.SubAssign() != null)
      Result(lhs.kind, s"${ lhs.value } -= ${ rhs.value }")
    else if (ctx.MulAssign() != null)
      Result(lhs.kind, s"${ lhs.value } *= ${ rhs.value }")
    else if (ctx.DivAssign() != null)
      Result(lhs.kind, s"${ lhs.value } /= ${ rhs.value }")
    else if (ctx.ModAssign() != null)
      Result(lhs.kind, s"${ lhs.value } %= ${ rhs.value }")
    else
      throw Error.Parse(ctx)
  }

  override def visitDeletion(ctx: CausticParser.DeletionContext): Result = {
    Result(CUnit, s"${ visitName(ctx.name()).value }.delete()")
  }

  override def visitLoop(ctx: CausticParser.LoopContext): Result = {
    val condition = visitExpression(ctx.expression())
    val body = Simplify(this.universe.child).visitBlock(ctx.block())
    Result(CUnit, s"While (${ condition.value }) {\n$body\n}")
  }

  override def visitConditional(ctx: CausticParser.ConditionalContext): Result = {
    val conditions = ctx.expression().asScala.map(visitExpression)
    val branches = ctx.block().asScala.map(Simplify(this.universe.child).visitBlock)
    val body = conditions.zip(branches) map { case (c, b) => s"If (${ c.value }) {\n${ b.value }\n} Else {" } mkString
    val last = if (ctx.Else() != null) branches.last else Result(CUnit, "None")
    last.copy(value = s"$body\n${ last.value }\n}")
  }

  override def visitExpression(ctx: CausticParser.ExpressionContext): Result =
    visitLogicalOrExpression(ctx.logicalOrExpression())

  override def visitLogicalOrExpression(ctx: CausticParser.LogicalOrExpressionContext): Result = {
    if (ctx.logicalOrExpression() == null) {
      // Recurse on higher precedence expressions.
      visitLogicalAndExpression(ctx.logicalAndExpression())
    } else {
      // Handle equality expressions.
      val lhs = visitLogicalOrExpression(ctx.logicalOrExpression())
      val rhs = visitLogicalAndExpression(ctx.logicalAndExpression())
      Result(CBoolean, s"${ lhs.value } || ${ rhs.value }")
    }
  }

  override def visitLogicalAndExpression(ctx: CausticParser.LogicalAndExpressionContext): Result = {
    if (ctx.logicalAndExpression() == null) {
      // Recurse on higher precedence expressions.
      visitEqualityExpression(ctx.equalityExpression())
    } else {
      // Handle equality expressions.
      val lhs = visitLogicalAndExpression(ctx.logicalAndExpression())
      val rhs = visitEqualityExpression(ctx.equalityExpression())
      Result(CBoolean, s"${ lhs.value } && ${ rhs.value }")
    }
  }

  override def visitEqualityExpression(ctx: CausticParser.EqualityExpressionContext): Result = {
    if (ctx.equalityExpression() == null) {
      // Recurse on higher precedence expressions.
      visitRelationalExpression(ctx.relationalExpression())
    } else {
      // Handle equality expressions.
      val lhs = visitEqualityExpression(ctx.equalityExpression())
      val rhs = visitRelationalExpression(ctx.relationalExpression())

      if (ctx.Equal() != null)
        Result(CBoolean, s"${ lhs.value } === ${ rhs.value }")
      else if (ctx.NotEqual() != null)
        Result(CBoolean, s"${ lhs.value } <> ${ rhs.value }")
      else
        throw Error.Parse(ctx)
    }
  }

  override def visitRelationalExpression(ctx: CausticParser.RelationalExpressionContext): Result = {
    if (ctx.relationalExpression() == null) {
      // Recurse on higher precedence expressions.
      visitAdditiveExpression(ctx.additiveExpression())
    } else {
      // Handle relational expressions.
      val lhs = visitRelationalExpression(ctx.relationalExpression())
      val rhs = visitAdditiveExpression(ctx.additiveExpression())

      if (ctx.LessThan() != null)
        Result(CBoolean, s"${ lhs.value } < ${ rhs.value }")
      else if (ctx.LessEqual() != null)
        Result(CBoolean, s"${ lhs.value } <= ${ rhs.value }")
      else if (ctx.GreaterThan() != null)
        Result(CBoolean, s"${ lhs.value } > ${ rhs.value }")
      else if (ctx.GreaterEqual() != null)
        Result(CBoolean, s"${ lhs.value } >= ${ rhs.value }")
      else
        throw Error.Parse(ctx)
    }
  }

  override def visitAdditiveExpression(ctx: CausticParser.AdditiveExpressionContext): Result = {
    if (ctx.additiveExpression() == null) {
      // Recurse on higher precedence expressions.
      visitMultiplicativeExpression(ctx.multiplicativeExpression())
    } else {
      // Handle additive expressions.
      val lhs = visitAdditiveExpression(ctx.additiveExpression())
      val rhs = visitMultiplicativeExpression(ctx.multiplicativeExpression())

      if (ctx.Add() != null)
        Result(lub(CInt, lhs.kind, rhs.kind), s"${ lhs.value } + ${ rhs.value }")
      else if (ctx.Sub() != null)
        Result(lub(CInt, lhs.kind, rhs.kind), s"${ lhs.value } - ${ rhs.value }")
      else
        throw Error.Parse(ctx)
    }
  }

  override def visitMultiplicativeExpression(ctx: CausticParser.MultiplicativeExpressionContext): Result = {
    if (ctx.multiplicativeExpression() == null) {
      // Recurse on higher precedence expressions.
      visitPrefixExpression(ctx.prefixExpression())
    } else {
      // Handle multiplicative expressions.
      val lhs = visitMultiplicativeExpression(ctx.multiplicativeExpression())
      val rhs = visitPrefixExpression(ctx.prefixExpression())

      if (ctx.Mul() != null)
        Result(lub(CInt, lhs.kind, rhs.kind), s"${ lhs.value } * ${ rhs.value }")
      else if (ctx.Div() != null)
        Result(lub(CInt, lhs.kind, rhs.kind), s"${ lhs.value } / ${ rhs.value }")
      else if (ctx.Mod() != null)
        Result(lub(CInt, lhs.kind, rhs.kind), s"${ lhs.value } % ${ rhs.value }")
      else
        throw Error.Parse(ctx)
    }
  }

  override def visitPrefixExpression(ctx: CausticParser.PrefixExpressionContext): Result = {
    val rhs = visitPrimaryExpression(ctx.primaryExpression())

    if (ctx.Add() != null)
      rhs
    else if (ctx.Sub() != null)
      rhs.copy(value = s"-${rhs.value}")
    else if (ctx.Not() != null)
      rhs.copy(value = s"!${rhs.value}")
    else
      rhs
  }

  override def visitPrimaryExpression(ctx: CausticParser.PrimaryExpressionContext): Result = {
    if (ctx.name() != null)
      visitName(ctx.name())
    else if (ctx.funcall() != null)
      visitFuncall(ctx.funcall())
    else if (ctx.constant() != null)
      visitConstant(ctx.constant())
    else if (ctx.expression() != null)
      visitExpression(ctx.expression())
    else
      throw Error.Parse(ctx)
  }

  override def visitName(ctx: CausticParser.NameContext): Result = {
    val names = ctx.Identifier().asScala.map(_.getText)
    this.universe.find(names.head) match {
      case Some(Variable(k)) =>
        names.drop(1).foldLeft(Result(k, names.head)) {
          case (Result(Pointer(Struct(_, r)), v), f) if r.contains(f) =>
            // Concatenate dot-delimited fields.
            Result(r(f), s"$v.get('$f)")
          case (Result(Struct(_, r), v), f) if r.contains(f) =>
            // Concatenate dot-delimited fields.
            Result(r(f), s"$v.get('$f)")
          case (Result(t, _), f) =>
            // Verify that every field is a member of the parent struct.
            throw Error.Type(s"${ t.name } does not have field $f.", Error.Trace(ctx))
        }
      case _ =>
        // Verify that the initial field is a variable.
        throw Error.Syntax(s"${ names.head } is not a variable defined in scope.", Error.Trace(ctx))
    }
  }

  override def visitFuncall(ctx: CausticParser.FuncallContext): Result = {
    if (ctx.`type`() == null || ctx.`type`().Ampersand() == null) {
      val name = ctx.Identifier().getText
      val args = ctx.expression().asScala.map(visitExpression).map(_.value).mkString(", ")

      this.universe.find(name) match {
        case Some(Function(_, r)) => Result(r.kind, s"$name$$Internal($args)")
        case _ => throw Error.Syntax(s"$name is not a function.", Error.Trace(ctx))
      }
    } else {
      val kind = this.universe.kind(ctx.`type`())
      Result(kind, s"""Reference[${ kind.name }$$Repr](Variable.Remote("${ ctx.String().getText }"))""")
    }
  }

  override def visitConstant(ctx: CausticParser.ConstantContext): Result = {
    if (ctx.True() != null)
      Result(CBoolean, s"true")
    else if (ctx.False() != null)
      Result(CBoolean, s"false")
    else if (ctx.Number() != null && ctx.Number().getText.contains('.'))
      Result(CDouble, ctx.Number().getText)
    else if (ctx.Number() != null)
      Result(CInt, ctx.Number().getText)
    else if (ctx.String() != null)
      Result(CString, ctx.String().getText)
    else if (ctx.Null() != null)
      Result(CUnit, "None")
    else
      throw Error.Parse(ctx)
  }

}
