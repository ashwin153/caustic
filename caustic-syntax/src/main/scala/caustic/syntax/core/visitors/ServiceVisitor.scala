package caustic.syntax.core.visitors

import caustic.grammar.CausticBaseVisitor
import caustic.grammar.CausticParser.{DeclarationContext, FunctionContext, ParameterContext, ParametersContext, RecordContext, ServiceContext}

/**
 *
 */
class ServiceVisitor extends CausticBaseVisitor[String] {

  override def visitService(ctx: ServiceContext): String =
    super.visitService(ctx)

  override def visitDeclaration(ctx: DeclarationContext): String =
    super.visitDeclaration(ctx)

  override def visitRecord(ctx: RecordContext): String =
    super.visitRecord(ctx)

  override def visitFunction(ctx: FunctionContext): String =
    super.visitFunction(ctx)

  override def visitParameters(ctx: ParametersContext): String =
    super.visitParameters(ctx)

  override def visitParameter(ctx: ParameterContext): String =
    super.visitParameter(ctx)

}
