package caustic.compiler.generate

import caustic.grammar._
import caustic.compiler.typing._

import scala.collection.JavaConverters._

/**
 *
 * @param universe
 */
case class DeclarationGenerator(
  universe: Universe
) extends CausticBaseVisitor[Unit] {

  override def visitDeclaration(ctx: CausticParser.DeclarationContext): Unit = {
    super.visitDeclaration(ctx)
  }

  override def visitRecord(ctx: CausticParser.RecordContext): Unit = {
    // Extract the fields of the record.
    var fields = ctx.parameters().parameter().asScala
      .map(p => (p.Identifier().getText, this.universe.getType(p.`type`().getText)))
      .toMap

    // Import fields of superclass.
    if (ctx.Extends() != null) {
      val parent = this.universe.getObject(ctx.Identifier(1).getText)
      fields = parent.fields ++ fields
    }

    // Add the record to the universe.
    // TODO: Implement constructors and json stringify.
    this.universe.putObject(ctx.Identifier(0).getText, fields)
  }

  override def visitService(ctx: CausticParser.ServiceContext): Unit = {
    DeclarationGenerator(universe.child(Seq(ctx.Identifier(0).getText))).visitChildren(ctx)
  }

  override def visitFunction(ctx: CausticParser.FunctionContext): Unit = {
    // Scope the arguments within the function body.
    val child = this.universe.child()
    val args = ctx.parameters().parameter().asScala
      .map(p => (child.scope(p.Identifier().getText), this.universe.getType(p.`type`().getText)))
      .toMap

    // Evaluate the body of the function and put the function into context.
    val tag = this.universe.getType(ctx.`type`().getText)
    val value = BlockGenerator(child).visitBlock(ctx.block())
    this.universe.putFunction(ctx.Identifier().getText, args, Result(tag, value))
  }

}