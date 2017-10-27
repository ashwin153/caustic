package caustic.compiler.generate

import caustic.compiler.typing.Universe
import caustic.grammar.{CausticBaseVisitor, CausticParser}

import scala.collection.JavaConverters._

/**
 *
 * @param universe
 */
case class ProgramGenerator(
  universe: Universe
) extends CausticBaseVisitor[String] {

  override def visitProgram(ctx: CausticParser.ProgramContext): String = {
    if (ctx.Module() != null) {
      // Scope the universe if a module is specified.
      val module = ctx.module(0).Identifier().asScala.map(_.getText)
      DeclarationGenerator(this.universe.child(module)).visitChildren(ctx)
    } else {
      // Otherwise, use the current universe by default.
      visitChildren(ctx)
    }
  }

}
