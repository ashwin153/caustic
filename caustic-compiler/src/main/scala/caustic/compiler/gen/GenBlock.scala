package caustic.compiler.gen

import caustic.compiler.error._
import caustic.compiler.reflect._
import caustic.grammar._

import scala.collection.JavaConverters._

/**
 * Generates a statically typed result from a block. Performs static type inference to determine
 * and validate the types of the various statements in the block.
 *
 * @param universe Type universe.
 */
case class GenBlock(universe: Universe) extends CausticBaseVisitor[Result] {

  override def visitBlock(ctx: CausticParser.BlockContext): Result = {
    // Determine the result of the block.
    val statements = ctx.statement().asScala.map(visitStatement)
    val result = statements.foldLeft(Result(CUnit, ""))((a, b) => b.copy(value = s"${ a.value }\n${ b.value }"))

    // Indent the result of the block.
    val lines = result.value.split("\n")
    result.copy(value = (1 to lines.size)
      .map(i => lines.take(i - 1).mkString.count(_ == '{') - lines.take(i).mkString.count(_ == '}'))
      .zipWithIndex
      .map { case (x, i) => " " * (2 * x) + lines(i) }
      .mkString("\n")
      .trim)
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
        this.universe.bind(lhs, CVariable(of))
        s"val $lhs = Variable.Local[$of](context.label())\n$lhs := $value"
      case Result(of: Record, value) =>
        this.universe.bind(lhs, CVariable(of))
        s"val $lhs = $value"
      case Result(any, _) =>
        throw Error.Type(s"Unable to define variable of type $any.", Trace(ctx.expression()))
    })
  }

  override def visitAssignment(ctx: CausticParser.AssignmentContext): Result = {
    val rhs = visitExpression(ctx.expression())
    val lhs = visitName(ctx.name())

    if (!isAssignable(lhs.of, rhs.of))
      throw Error.Type(s"Expected ${ lhs.of }, but was ${ rhs.of }.", Trace(ctx))
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
      case Result(CPointer(_: Primitive), v) => Result(CUnit, s"$v := Null")
      case Result(CPointer(_), v) => Result(CUnit, s"$v.delete()")
      case Result(_: Primitive, v) => Result(CUnit, s"$v := Null")
      case Result(_, v) => Result(CUnit, s"$v.delete()")
    }
  }

  override def visitLoop(ctx: CausticParser.LoopContext): Result = {
    val condition = visitExpression(ctx.expression())
    val body = GenBlock(this.universe.child).visitBlock(ctx.block())

    if (condition.of != CBoolean)
      throw Error.Type(s"Found ${ condition.of }, but expected Boolean.", Trace(ctx.expression()))
    else
      Result(CUnit, s"While (${ condition.value }) {\n${ body.value }\n}")
  }

  override def visitConditional(ctx: CausticParser.ConditionalContext): Result = {
    val branches = ctx.block().asScala.map(GenBlock(this.universe.child).visitBlock)
    val last = if (ctx.Else() != null) branches.last else Result(CUnit, "Null")
    val conditions = ctx.expression().asScala map { cmp =>
      val result = visitExpression(cmp)
      if (result.of != CBoolean)
        throw Error.Type(s"Found ${ result.of }, but expected Boolean.", Trace(cmp))
      else
        result.value
    }

    Result(last.of, conditions.zip(branches)
      .map { case (c, b) => s"If ($c) {\n${ b.value }\n} Else {\n" }
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

      if (!isBoolean(lhs.of))
        throw Error.Type(s"Found type ${ lhs.of }, but expected Boolean.", Trace(ctx.logicalOrExpression()))
      else if (!isBoolean(rhs.of))
        throw Error.Type(s"Found type ${ rhs.of }, but expected Boolean.", Trace(ctx.logicalAndExpression()))
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

      if (!isBoolean(lhs.of))
        throw Error.Type(s"Found type ${ lhs.of }, but expected Boolean.", Trace(ctx.logicalAndExpression()))
      else if (!isBoolean(rhs.of))
        throw Error.Type(s"Found type ${ rhs.of }, but expected Boolean.", Trace(ctx.equalityExpression()))
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

      if (ctx.Add() != null && isPrimitive(lhs.of) && isPrimitive(rhs.of))
        Result(lub(lhs.of, rhs.of), s"${ lhs.value } + ${ rhs.value }")
      else if (ctx.Sub() != null && isNumeric(lhs.of) && isNumeric(rhs.of))
        Result(lub(lhs.of, rhs.of), s"${ lhs.value } - ${ rhs.value }")
      else
        throw Error.Type(s"Unexpected type ${ lub(lhs.of, rhs.of) }.", Trace(ctx))
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
        throw Error.Type(s"Found type ${ lhs.of }, but expected numeric.", Trace(ctx.multiplicativeExpression()))
      else if (!isNumeric(rhs.of))
        throw Error.Type(s"Found type ${ rhs.of }, but expected numeric.", Trace(ctx.prefixExpression()))
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
      case Result(CFunction(_, args, returns), function) if isPassable(args, arguments.map(_.of)) =>
        Result(returns, s"$function(${ arguments.map(_.value).mkString(", ") })")
      case Result(any, _) =>
        throw Error.Type(s"$any is not a function", Trace(ctx.name()))
    }
  }

  override def visitName(ctx: CausticParser.NameContext): Result = {
    val names = ctx.Identifier().asScala.map(_.getText)

    this.universe.find(s"${ names.head }${ if (ctx.Ampersand() != null) "&" else "" }") match {
      case Some(service: CService) if names.size == 2 && service.functions.contains(names(1)) =>
        Result(service.functions(names(1)), s"${ names.head }.${ service.functions(names(1)).name }")
      case Some(variable: CVariable) =>
        names.drop(1).foldLeft(Result(variable.of, names.head)) {
          case (Result(CPointer(struct: CStruct), value), field) if struct.fields.contains(field) =>
            Result(struct.fields(field), s"$value.get('$field)")
          case (Result(struct: CStruct, value), field) if struct.fields.contains(field) =>
            Result(struct.fields(field), s"$value.get('$field)")
          case (Result(CPointer(builtIn: BuiltIn), value), field) if builtIn.fields.contains(field) =>
            Result(builtIn.fields(field), s"$value.$field")
          case (Result(builtIn: BuiltIn, value), field) if builtIn.fields.contains(field) =>
            Result(builtIn.fields(field), s"$value.$field")
          case (Result(any, _), field) =>
            throw Error.Type(s"$field is not a member of $any.", Trace(ctx))
        }
      case Some(function: CFunction) if names.size == 1 =>
        Result(function, function.name)
      case Some(_: CPointer) | Some(_: Record) if names.size == 1 =>
        this.universe.find(s"${ names.head }$$Constructor") match {
          case Some(function: CFunction) => Result(function, function.name)
          case _ => throw Error.Type(s"Cannot find constructor for ${ names.head }.", Trace(ctx))
        }
      case Some(any) =>
        throw Error.Type(s"$any is not a valid name.", Trace(ctx))
      case _ =>
        throw Error.Type(s"Unknown binding ${ names.mkString }.", Trace(ctx))
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
      Result(CNull, "Null")
    else
      throw Error.Parse(ctx)
  }

}