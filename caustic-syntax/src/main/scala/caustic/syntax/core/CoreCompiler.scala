package caustic.syntax.core

import caustic.syntax.Compiler
import caustic.grammar.CausticParser.{DeclarationContext, FunctionContext, ServiceContext}

/**
 *
 */
class CoreCompiler extends Compiler {

  override def visitService(ctx: ServiceContext): String = super.visitService(ctx)

  override def visitDeclaration(ctx: DeclarationContext): String = super.visitDeclaration(ctx)

  override def visitFunction(ctx: FunctionContext): String = super.visitFunction(ctx)

}
