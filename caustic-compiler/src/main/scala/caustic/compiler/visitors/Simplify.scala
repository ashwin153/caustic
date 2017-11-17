package caustic.compiler.visitors

import caustic.compiler.types._
import caustic.compiler.{Handler, TypeError, types}
import caustic.grammar.{CausticBaseVisitor, CausticParser}

import org.antlr.v4.runtime.misc.ParseCancellationException

import scala.collection.JavaConverters._

/**
 * A basic block evaluator. Reduces a statement or collection of statements into a [[Result]], whose
 * value corresponds to a transaction and whose tag corresponds to a compiler generated [[Type]].
 *
 * @param handler Exception [[Handler]].
 * @param universe Known [[Universe]].
 */
case class Simplify(
  handler: Handler,
  universe: Universe
) extends CausticBaseVisitor[Result] {

  override def visitBlock(ctx: CausticParser.BlockContext): Result =
    ctx.statement().asScala
      .map(visitStatement)
      .reduce((a, b) => Result(b.tag, s"""cons(${ a.value }, ${ b.value })"""))

  override def visitStatement(ctx: CausticParser.StatementContext): Result =
    visitChildren(ctx)

  override def visitRollback(ctx: CausticParser.RollbackContext): Result = {
    val message = visitExpression(ctx.expression())
    Result(Null, s"""rollback(${ message.value })""")
  }

  override def visitDefinition(ctx: CausticParser.DefinitionContext): Result = {
    // Extract the name of the variable.
    val variable = ctx.Identifier().getText

    // Determine the type of the variable, and add it to the universe.
    val rhs = visitExpression(ctx.expression())
    this.universe.putVariable(variable, rhs.tag)

    // Initialize the value of the variable.
    val lhs = this.universe.getVariable(variable).get
    Result(Null, s"""store("${ lhs.key }", ${ rhs.value })""")
  }

  override def visitAssignment(ctx: CausticParser.AssignmentContext): Result = {
    // Determine the value of the variable.
    var rhs = visitExpression(ctx.expression())
    val cur = Simplify.get(visitName(ctx.name()))

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
    val lhs = visitName(ctx.name())
    if (lhs.tag == lub(lhs.tag, rhs.tag)) {
      Result(Null, Simplify.copy(lhs, rhs))
    } else {
      this.handler.report(TypeError, s"Expected ${ lhs.tag } for ${ ctx.name() }, but was ${ rhs.tag }.", ctx.name())
      throw new ParseCancellationException()
    }
  }

  override def visitDeletion(ctx: CausticParser.DeletionContext): Result = {
    Result(Null, Simplify.delete(visitName(ctx.name())))
  }

  override def visitLoop(ctx: CausticParser.LoopContext): Result = {
    // Load the loop condition and body.
    val condition = visitExpression(ctx.expression())
    val block = Simplify(this.handler, this.universe.child()).visitBlock(ctx.block())

    // Serialize the loop as a repeat expression.
    Result(Null, s"""repeat(${ condition.value }, $block)""")
  }

  override def visitConditional(ctx: CausticParser.ConditionalContext): Result = {
    // Construct the if/elif/else branches.
    val blocks = ctx.block().asScala.map(Simplify(this.handler, this.universe.child()).visitBlock)
    val compares = ctx.expression().asScala.map(visitExpression)
    val branches = compares.zip(blocks).map { case (c, b) => s"""branch(${ c.value }, ${ b.value }, """ }

    // Append the else block and closing parenthesis.
    val last = if (ctx.Else() != null) blocks.last else Result(Null, "None")
    Result(last.tag, branches.mkString + last.value + ")" * branches.size)
  }

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
          Result(lub(lhs.tag, rhs.tag), s"""notEqual(${ lhs.value }, ${ rhs.value })""")
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
        else if (ctx.Div() != null && lhs.tag == types.Int && rhs.tag == types.Int)
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
      Simplify.get(visitName(ctx.name()))
    else if (ctx.funcall() != null)
      visitFuncall(ctx.funcall())
    else if (ctx.constant() != null)
      visitConstant(ctx.constant())
    else if (ctx.expression() != null)
      visitExpression(ctx.expression())
    else
      visitChildren(ctx)

  override def visitName(ctx: CausticParser.NameContext): Result = {
    ctx.Identifier().asScala.drop(1).map(_.getText).foldLeft {
      // Extract the name of the variable.
      val variable = ctx.Identifier(0).getText

      // Determine the key referenced by the variable.
      this.universe.getVariable(variable) match {
        case Some(Variable(_, k, x: Pointer)) =>
          Result(x, s"""load("$k")""")
        case Some(Variable(_, k, x)) =>
          Result(x, s"""text("$k")""")
        case None =>
          this.handler.report(TypeError, s"Undefined variable $variable.", ctx.Identifier(0))
          throw new ParseCancellationException()
      }
    } { (key, field) =>
      // Determine the type of the field.
      val symbol = key.tag match {
        case Pointer(x: Record) =>
          x.fields(field)
        case Record(f) =>
          f(field)
        case _ =>
          this.handler.report(TypeError, s"Field $field does not exist for ${ key.tag }.", ctx)
          throw new ParseCancellationException()
      }

      (key.tag, symbol.datatype) match {
        case (_: Pointer, x: Pointer) =>
          // Automatically dereference nested pointers.
          Result(x, s"""read(add(${ key.value }, text("@$field")))""")
        case (_: Pointer, x: Record) =>
          // Concatenate the field name with the key.
          Result(Pointer(x), s"""add(${ key.value }, text("@$field"))""")
        case (_: Pointer, x: Primitive) =>
          // Automatically dereference primitive pointers.
          Result(Pointer(x), s"""add(${ key.value }, text("@$field"))""")
        case (_: Record, x: Pointer) =>
          // Load the pointer to the field.
          Result(x, s"""load(add(${ key.value }, text("@$field")))""")
        case (_: Record, x: Record) =>
          // Concatenate the field name with the key.
          Result(x, s"""add(${ key.value }, text("@$field"))""")
        case (_: Record, x: Primitive) =>
          // Load the primitive field.
          Result(x, s"""add(${ key.value }, text("@$field"))""")
      }
    }
  }

  override def visitFuncall(ctx: CausticParser.FuncallContext): Result = {
    // Extract the function name and context.
    val function = ctx.Identifier().asScala.last
    val context = ctx.Identifier().asScala
      .dropRight(1)
      .map(_.getText)
      .foldLeft(this.universe)((u, s) => u.child(s))

    context.getFunction(function.getText) match {
      case Some(Function(_, args, returns, body)) =>
        // Initialize the arguments of the function.
        val initialize = args.zip(ctx.expression().asScala.map(visitExpression)) map {
          case (Argument(_, k, Alias(_, x)), Result(y, v)) if x == lub(x, y) =>
            s"""store($k, $v)"""
          case (Argument(n, _, Alias(_, x)), Result(y, _)) =>
            this.handler.report(TypeError, s"Expected $n to be $x, but received $y.", function)
            throw new ParseCancellationException()
        }

        // Copy the function body after argument initialization.
        Result(returns.datatype, initialize.foldRight(body.value)((a, b) => s"""cons($a, $b)"""))
      case None =>
        this.handler.report(TypeError, s"Function ${ function.getText } does not exist.", function)
        throw new ParseCancellationException()
    }
  }

  override def visitConstant(ctx: CausticParser.ConstantContext): Result = {
    if (ctx.True() != null)
      Result(Boolean, s"""flag(true)""")
    else if (ctx.False() != null)
      Result(Boolean, s"""flag(false)""")
    else if (ctx.Number() != null && ctx.Number().getText.contains('.'))
      Result(Double, s"""real(${ ctx.Number().getText.toDouble })""")
    else if (ctx.Number() != null)
      Result(Int, s"""real(${ ctx.Number().getText.toDouble })""")
    else if (ctx.String() != null)
      Result(String, s"""text(${ ctx.String().getText })""")
    else if (ctx.Null() != null)
      Result(Null, s"""None""")
    else
      visitChildren(ctx)
  }

}

object Simplify {

  /**
   * Gets the value of a [[Result]] by loading [[Primitive]] types and dereferencing [[Pointer]]
   * types. All other types are passed through normally.
   *
   * @param result [[Result]] to fetch.
   * @return Value of [[Result]].
   */
  def get(result: Result): Result = result.tag match {
    case x: Primitive =>
      // Automatically load primitives.
      Result(x, s"""load(${ result.value })""")
    case Pointer(x: Primitive) =>
      // Automatically read primitive pointers.
      Result(x, s"""read(${ result.value })""")
    case _ =>
      // Otherwise, pass through results normally.
      result
  }

  /**
   * Returns the fields of the [[Record]] contained in the [[Result]].
   *
   * @param result Result.
   * @return Fields of contained record.
   */
  def fields(result: Result): Iterable[Result] = result.tag match {
    case Record(fields) =>
      val names = fields.keys.map(f => s"""add(${ result.value }, "@$f")""")
      val types = fields.values.map(_.datatype).map(t => if (t.isInstanceOf[Pointer]) String else t)
      types.zip(names).map { case (a, b) => Result(a, b) }
    case Pointer(Record(fields)) =>
      val names = fields.keys.map(f => s"""add(${ result.value }, "@$f")""")
      val types = fields.values.map(_.datatype).map(t => if (t.isInstanceOf[Pointer]) Pointer(String) else t)
      types.zip(names).map { case (a, b) => Result(a, b) }
  }

  /**
   * Returns a transaction that copies the value of the right-hand side [[Result]] to the left-hand
   * side [[Result]]. Recursively copies the fields of a [[Record]].
   *
   * @param lhs Destination.
   * @param rhs Source.
   * @return Copy transaction.
   */
  def copy(lhs: Result, rhs: Result): String = (lhs.tag, rhs.tag) match {
    case (x: Primitive, y: Primitive) =>
      s"""store(${ lhs.value }, ${ rhs.value })"""
    case (x: Primitive, Pointer(y: Primitive)) =>
      // Dereference primitive pointers.
      s"""store(${ lhs.value }, read(${ rhs.value }))"""
    case (Pointer(x: Primitive), y: Primitive) =>
      // Copy primitive values.
      s"""write(${ lhs.value }, ${ rhs.value })"""
    case (Pointer(x: Primitive), Pointer(y: Primitive)) =>
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
   * Returns a transaction that deletes the contents of the specified result. Recursively deletes
   * the fields of a [[Record]].
   *
   * @param lhs Value.
   * @return Delete transaction.
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


