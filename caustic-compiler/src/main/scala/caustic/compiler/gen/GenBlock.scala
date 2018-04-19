package caustic.compiler.gen

import caustic.compiler.Error
import caustic.compiler.reflect._
import caustic.grammar.{CausticBaseVisitor, CausticParser}

import scala.collection.JavaConverters._

case class GenBlock(universe: Universe) extends CausticBaseVisitor[Result] {

  override def visitBlock(ctx: CausticParser.BlockContext): Result = {
    val statements = ctx.statement().asScala.map(visitStatement)
    statements.reduceLeft((a, b) => b.copy(value = s"${a.value}\n${b.value}"))
  }

  override def visitStatement(ctx: CausticParser.StatementContext): Result = {
    visitChildren(ctx)
  }

  override def visitRollback(ctx: CausticParser.RollbackContext): Result = {
    val message = visitExpression(ctx.expression())
    Result(CUnit, s"Rollback (${ message.value })")
  }

  override def visitDefinition(ctx: CausticParser.DefinitionContext): Result = {
    val rhs = visitExpression(ctx.expression())
    val lhs = ctx.Identifier().getText

    // Construct a variable definition and bind it to the universe.
    Result(CUnit, rhs match {
      case Result(of: Primitive, value) =>
        this.universe.bind(lhs, Variable(of))
        s"val $lhs = Variable.Local[$of](context.label())\n$lhs := $value"
      case Result(of: Struct, value) =>
        this.universe.bind(lhs, Variable(of))
        s"val $lhs = $value"
      case Result(any, _) =>
        throw Error.Type(s"Unable to define variable of type $any.", Error.Trace(ctx.expression()))
    })
  }

  override def visitAssignment(ctx: CausticParser.AssignmentContext): Result = {
    val rhs = visitExpression(ctx.expression())
    val lhs = visitName(ctx.name())

    if (lub(lhs.of, rhs.of) != lhs.of)
      throw Error.Type(s"Expected ${ lhs.of }, but was ${ rhs.of }.", Error.Trace(ctx))
    else if (ctx.Assign() != null)
      Result(lhs.of, s"${ lhs.value } := ${ rhs.value }")
    else if (ctx.AddAssign() != null)
      Result(lhs.of, s"${ lhs.value } += ${ rhs.value }")
    else if (ctx.SubAssign() != null)
      Result(lhs.of, s"${ lhs.value } -= ${ rhs.value }")
    else if (ctx.MulAssign() != null)
      Result(lhs.of, s"${ lhs.value } *= ${ rhs.value }")
    else if (ctx.DivAssign() != null)
      Result(lhs.of, s"${ lhs.value } /= ${ rhs.value }")
    else if (ctx.ModAssign() != null)
      Result(lhs.of, s"${ lhs.value } %= ${ rhs.value }")
    else
      throw Error.Parse(ctx)
  }

  override def visitDeletion(ctx: CausticParser.DeletionContext): Result = {
    visitName(ctx.name()) match {
      case Result(Pointer(_: Primitive), v) => Result(CUnit, s"$v := Null")
      case Result(Pointer(_), v) => Result(CUnit, s"$v.delete()")
      case Result(_: Primitive, v) => Result(CUnit, s"$v := Null")
      case Result(_, v) => Result(CUnit, s"$v.delete()")
    }
  }

  override def visitLoop(ctx: CausticParser.LoopContext): Result = {
    val condition = visitExpression(ctx.expression())
    val body = GenBlock(this.universe.child).visitBlock(ctx.block())

    if (condition.of != CBoolean)
      throw Error.Type(s"Found ${ condition.of }, but expected Boolean.", Error.Trace(ctx.expression()))
    else
      Result(CUnit, s"While (${ condition.value }) {\n${ body.value }\n}")
  }

  override def visitConditional(ctx: CausticParser.ConditionalContext): Result = {
    val branches = ctx.block().asScala.map(GenBlock(this.universe.child).visitBlock)
    val last = if (ctx.Else() != null) branches.last else Result(CUnit, "Null")
    val conditions = ctx.expression().asScala map { cmp =>
      val result = visitExpression(cmp)
      if (result.of != CBoolean)
        throw Error.Type(s"Found ${ result.of }, but expected Boolean.", Error.Trace(cmp))
      else
        result.value
    }

    Result(last.of, conditions.zip(branches)
      .map { case (c, b) => s"If ($c) {\n${ b.value }\n} Else {" }
      .mkString
      .concat(last.value)
      .concat("\n}" * conditions.size))
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

      if (!isSubtype(lhs.of, CBoolean))
        throw Error.Type(s"Found type ${ lhs.of }, but expected Boolean.", Error.Trace(ctx.logicalOrExpression()))
      else if (!isSubtype(rhs.of, CBoolean))
        throw Error.Type(s"Found type ${ rhs.of }, but expected Boolean.", Error.Trace(ctx.logicalAndExpression()))
      else
        Result(lub(lhs.of, rhs.of), s"${ lhs.value } || ${ rhs.value }")
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

      if (!isSubtype(lhs.of, CBoolean))
        throw Error.Type(s"Found type ${ lhs.of }, but expected Boolean.", Error.Trace(ctx.logicalAndExpression()))
      else if (!isSubtype(rhs.of, CBoolean))
        throw Error.Type(s"Found type ${ rhs.of }, but expected Boolean.", Error.Trace(ctx.equalityExpression()))
      else
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

      if (ctx.Add() != null && isSubtype(lhs.of, CString) && isSubtype(rhs.of, CString))
        Result(lub(lhs.of, rhs.of), s"${ lhs.value } + ${ rhs.value }")
      else if (ctx.Sub() != null && isNumeric(lhs.of) && isNumeric(rhs.of))
        Result(lub(lhs.of, rhs.of), s"${ lhs.value } - ${ rhs.value }")
      else
        throw Error.Type(s"Unexpected type ${ lub(lhs.of, rhs.of) }.", Error.Trace(ctx))
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

      if (!isNumeric(lhs.of))
        throw Error.Type(s"Found type ${ lhs.of }, but expected numeric.", Error.Trace(ctx.multiplicativeExpression()))
      else if (!isNumeric(rhs.of))
        throw Error.Type(s"Found type ${ rhs.of }, but expected numeric.", Error.Trace(ctx.prefixExpression()))
      else if (ctx.Mul() != null)
        Result(lub(lhs.of, rhs.of), s"${ lhs.value } * ${ rhs.value }")
      else if (ctx.Div() != null && lub(lhs.of, rhs.of) == CDouble)
        Result(CDouble, s"${ lhs.value } / ${ rhs.value }")
      else if (ctx.Div() != null && lub(lhs.of, rhs.of) == CInt)
        Result(CInt, s"floor(${ lhs.value } / ${ rhs.value })")
      else if (ctx.Mod() != null)
        Result(lub(lhs.of, rhs.of), s"${ lhs.value } % ${ rhs.value }")
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

  override def visitFuncall(ctx: CausticParser.FuncallContext): Result = {
    val arguments = ctx.expression().asScala.map(visitExpression)

    visitName(ctx.name()) match {
      case Result(Function(_, args, returns), function) if args == arguments.map(_.of) =>
        Result(returns, s"$function(${ arguments.map(_.value).mkString(", ") })")
      case Result(any, _) =>
        throw Error.Type(s"$any is not a function", Error.Trace(ctx.name()))
    }
  }

  override def visitName(ctx: CausticParser.NameContext): Result = {
    val names = ctx.Identifier().asScala.map(_.getText)

    this.universe.find(names.head) match {
      case Some(service: Service) if names.size == 2 && service.functions.contains(names(1)) =>
        Result(service.functions(names(1)), s"${ names.head }.${ service.functions(names(1)).name }")
      case Some(variable: Variable) =>
        names.drop(1).foldLeft(Result(variable.of, names.head)) {
          case (Result(Pointer(struct: Struct), value), field) if struct.fields.contains(field) =>
            Result(struct.fields(field), s"$value.get('$field)")
          case (Result(defined: Defined, value), field) if defined.fields.contains(field) =>
            Result(defined.fields(field), s"$value.get('$field)")
          case (Result(builtIn: BuiltIn, value), field) if builtIn.fields.contains(field) =>
            Result(builtIn.fields(field), s"$value.$field")
          case (Result(any, _), field) =>
            throw Error.Type(s"$field is not a member of $any.", Error.Trace(ctx))
        }
      case Some(function: Function) if names.size == 1 =>
        Result(function, function.name)
      case Some(_: Pointer) | Some(_: Struct) if names.size == 1 =>
        this.universe.find(s"${ names.head }$$Constructor") match {
          case Some(function: Function) => Result(function, function.name)
          case _ => throw Error.Type(s"Cannot find constructor for ${ names.head }.", Error.Trace(ctx))
        }
      case Some(any) =>
        throw Error.Type(s"$any is not a valid name.", Error.Trace(ctx))
      case _ =>
        throw Error.Type(s"Unknown binding ${ names.mkString }.", Error.Trace(ctx))
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
      Result(CString, s"string(${ ctx.String().getText })")
    else if (ctx.Null() != null)
      Result(CUnit, "Null")
    else
      throw Error.Parse(ctx)
  }

}