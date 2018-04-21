package caustic.compiler.gen

import caustic.compiler.error._
import caustic.compiler.reflect._
import caustic.grammar._

/**
 * Generates a static type from a type.
 *
 * @param universe Type universe.
 */
case class GenType(universe: Universe) extends CausticBaseVisitor[Type] {

  override def visitBlock(ctx: CausticParser.BlockContext): Type =
    GenBlock(this.universe).visitBlock(ctx).of

  override def visitType(ctx: CausticParser.TypeContext): Simple =
    this.universe.find(ctx.getText.replaceAll("\\s", "")) match {
      case Some(value: Simple) =>
        value
      case Some(any) =>
        throw Error.Type(s"Field cannot be of type $any.", Trace(ctx))
      case None =>
        throw Error.Type(s"Unknown type ${ ctx.getText }.", Trace(ctx))
    }

}
