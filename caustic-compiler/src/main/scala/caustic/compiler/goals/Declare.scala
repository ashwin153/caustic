package caustic.compiler.goals

import caustic.compiler.Goal
import caustic.compiler.types.{Record, Universe}
import caustic.compiler.types.{Record, Result}
import caustic.grammar.{CausticBaseVisitor, CausticParser}
import scala.collection.JavaConverters._
import scala.util.Try

/**
 *
 * @param universe
 */
case class Declare(
  universe: Universe
) extends CausticBaseVisitor[Universe] with Goal[Universe] {

  override def execute(parser: CausticParser): Try[Universe] =
    Try(visitProgram(parser.program()))

  override def visitProgram(ctx: CausticParser.ProgramContext): Universe =
    if (ctx.Module() != null) {
      // Scope the universe if a module is specified.
      val module = ctx.module(0).Identifier().asScala.map(_.getText)
      val child = this.universe.child(module: _*)
      Declare(child).visitChildren(ctx)
    } else {
      // Otherwise, use the current universe by default.
      visitChildren(ctx)
    }

  override def visitRecord(ctx: CausticParser.RecordContext): Universe = {
    // Extract the fields of the object.
    var fields = ctx.parameters().parameter().asScala
      .map(p => (p.Identifier().getText, this.universe.getAlias(p.`type`().getText)))
      .toMap

    // Import fields of superclass.
    if (ctx.Extends() != null) {
      val parent = this.universe.getAlias(ctx.Identifier(1).getText)
      fields = parent.datatype.asInstanceOf[Record].fields ++ fields
    }

    // Add the object to the universe.
    this.universe.putAlias(ctx.Identifier(0).getText, Record(fields))
    this.universe
  }

  override def visitService(ctx: CausticParser.ServiceContext): Universe = {
    this.universe.putService(ctx.Identifier(0).getText, ctx.function().asScala map { f =>
      // Extract the function arguments.
      val args = f.parameters().parameter().asScala
        .map(p => (p.Identifier().getText, this.universe.getAlias(p.`type`().getText)))
        .toMap

      // Add the function to the universe.
      val name = f.Identifier().getText
      val tag = this.universe.getAlias(f.`type`().getText)
      this.universe.putFunction(name, args, tag)(func => Simplify(func).visitBlock(f.block()))
      this.universe.getFunction(name)
    })

    this.universe
  }

}