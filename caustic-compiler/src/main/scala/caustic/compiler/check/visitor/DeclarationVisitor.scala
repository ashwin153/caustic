package caustic.compiler.check.visitor

import caustic.compiler.check.{Result, Universe}
import caustic.grammar._
import scala.collection.JavaConverters._

/**
 *
 * @param universe
 */
case class DeclarationVisitor(
  universe: Universe
) extends CausticBaseVisitor[Unit] {

  override def visitProgram(ctx: CausticParser.ProgramContext): Unit = {
    if (ctx.Module() != null) {
      // Scope the universe if a module is specified.
      val module = ctx.module(0).Identifier().asScala.map(_.getText)
      DeclarationVisitor(this.universe.child(module: _*)).visitChildren(ctx)
    } else {
      // Otherwise, use the current universe by default.
      visitChildren(ctx)
    }
  }

  override def visitDeclaration(ctx: CausticParser.DeclarationContext): Unit = {
    super.visitDeclaration(ctx)
  }

  override def visitRecord(ctx: CausticParser.RecordContext): Unit = {
    // Extract the fields of the object.
    var fields = ctx.parameters().parameter().asScala
      .map(p => (p.Identifier().getText, this.universe.getType(p.`type`().getText)))
      .toMap

    // Import fields of superclass.
    if (ctx.Extends() != null) {
      val parent = this.universe.getRecord(ctx.Identifier(1).getText)
      fields = parent.fields ++ fields
    }

    // Add the object to the universe.
    this.universe.putRecord(ctx.Identifier(0).getText, fields)
  }

  override def visitService(ctx: CausticParser.ServiceContext): Unit = {
    DeclarationVisitor(universe.child(ctx.Identifier(0).getText)).visitChildren(ctx)
  }

  override def visitFunction(ctx: CausticParser.FunctionContext): Unit = {
    // Extract the function arguments.
    val args = ctx.parameters().parameter().asScala
      .map(p => (p.Identifier().getText, this.universe.getType(p.`type`().getText)))
      .toMap

    // Add the function to the universe.
    val name = ctx.Identifier().getText
    val tag = this.universe.getType(ctx.`type`().getText)
    this.universe.putFunction(name, args, f => Result(tag, BlockVisitor(f).visitBlock(ctx.block())))
  }

}