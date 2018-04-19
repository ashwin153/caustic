package caustic.compiler.gen

import caustic.compiler.reflect._
import caustic.grammar.{CausticBaseVisitor, CausticParser}

case class GenFunction(universe: Universe) extends CausticBaseVisitor[(String, Function)] {

  override def visitFunction(ctx: CausticParser.FunctionContext): (String, Function) = {
    val name = ctx.Identifier().getText
    val args = GenParameters(this.universe).visitParameters(ctx.parameters()).values.toList
    val returns = GenType(this.universe).visitType(ctx.`type`())
    val function = Function(s"$name$$Internal", args, returns)
    this.universe.bind(name, function)
    name -> function
  }

}
