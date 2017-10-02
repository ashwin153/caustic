package caustic.syntax
package compiler

import caustic.grammar.CausticBaseVisitor
import caustic.grammar.CausticParser._

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 *
 * @param namespace
 * @param symbols
 */
case class BlockVisitor(
  symbols: Map[String, Symbol],
  namespace: String
) extends CausticBaseVisitor[String] {

  // Local symbol table.
  val locals: mutable.Map[String, Symbol] = mutable.Map.empty

  // Monotonically increasing label number.
  var label: Int = 0

  /**
   * Returns the result of visiting the specified BlockContext. Visitor is scoped using the
   * monotonically increasing label number and nested blocks inherit the local symbols of their
   * parent block.
   *
   * @param ctx BlockContext to visit.
   * @return Result of visiting BlockContext.
   */
  def scope(ctx: BlockContext): String = {
    val frame = this.namespace + "$" + this.label
    this.label += 1
    BlockVisitor(this.symbols ++ this.locals, frame).visitBlock(ctx)
  }

  override def visitBlock(ctx: BlockContext): String =
    ctx.statement().asScala
      .map(visitStatement)
      .foldLeft("Empty")((a, b) => s"""cons($a, $b)""")

  override def visitRollback(ctx: RollbackContext): String =
    s"""rollback(${ExpressionVisitor(this.symbols).visitExpression(ctx.expression())})"""

  override def visitDefinition(ctx: DefinitionContext): String = {
    super.visitDefinition(ctx)
  }

  override def visitAssignment(ctx: AssignmentContext): String = {
    this.symbols(ctx.variable().Identifier(0).getText) match {
      case Variable(name, true, Primitive) =>
        s"""store(text("$name"), ${visitValue(ctx.value)})"""
      case Variable(name, true, Record(f)) =>
        // Set every field of the record equal to the provided record.

      case Variable(name, true, Reference(Primitive)) =>
        s"""write(text("$name", ${visitValue(ctx.value)})"""
      case Variable(name, true, Reference(Record(f))) =>
        // Set every field of the referenced record equal to the provided record.

    }

    super.visitChildren(ctx)
  }

  override def visitValue(ctx: ValueContext): String = {
    if (ctx.expression() != null)
      ExpressionVisitor(this.symbols).visitExpression(ctx.expression())
    else if (ctx.variable() != null)
      ExpressionVisitor(this.symbols).visitVariable(ctx.variable())
    else if (ctx.funcall() != null)
      ExpressionVisitor(this.symbols).visitFuncall(ctx.funcall())
    else
      super.visitChildren(ctx)
  }

  override def visitDeletion(ctx: DeletionContext): String = {
    case Variable(name, true, Primitive) =>
      // Set the value of the variable to be empty.
      s"""store(text("$name"), Empty)"""
    case Variable(name, true, Record(f)) =>
      // Set every field of the record equal to the provided record.

    case Variable(name, true, Reference(Primitive)) =>
      // Set the value of the referenced primitive to be empty.
      s"""write(text("$name", Empty)"""
    case Variable(name, true, Reference(Record(f))) =>
      // Set every field of the referenced record equal to the provided record.

  }

  override def visitLoop(ctx: LoopContext): String = {
    // Load the loop condition and body.
    val condition = ExpressionVisitor(this.symbols).visitExpression(ctx.expression())
    val body = scope(ctx.block())

    // Serialize the loop as a repeat expression.
    s"""repeat($condition, $body)"""
  }

  override def visitConditional(ctx: ConditionalContext): String = {
    // Reduce the if and elif blocks.
    val branches = ctx.expression().asScala
      .map(ExpressionVisitor(this.symbols).visitExpression)
      .zip(ctx.block().asScala.map(scope))
      .map { case (a, b) => s"""branch($a, $b""" }
      .mkString

    // Optionally append the else block and add closing parenthesis.
    val size = ctx.block().size()
    val last = if (ctx.Else() != null) scope(ctx.block(size)) else "Empty"
    branches + last + Seq.fill(size)(")")
  }

}