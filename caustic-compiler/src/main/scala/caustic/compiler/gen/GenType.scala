package caustic.compiler.gen

import caustic.compiler.Error
import caustic.compiler.reflect._
import caustic.grammar.{CausticBaseVisitor, CausticParser}

case class GenType(universe: Universe) extends CausticBaseVisitor[Type] {

  override def visitBlock(ctx: CausticParser.BlockContext): Type =
    GenBlock(this.universe).visitBlock(ctx).of

  override def visitType(ctx: CausticParser.TypeContext): Value =
    this.universe.find(ctx.getText.replaceAll("\\s", "")) match {
      case Some(value: Value) =>
        value
      case Some(any) =>
        throw Error.Type(s"Field cannot be of type $any.", Error.Trace(ctx))
      case None =>
        throw Error.Type(s"Unknown type ${ ctx.getText }.", Error.Trace(ctx))
    }

}
