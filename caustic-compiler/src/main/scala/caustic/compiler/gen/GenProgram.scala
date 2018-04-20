package caustic.compiler.gen

import caustic.compiler.reflect._
import caustic.grammar.{CausticBaseVisitor, CausticParser}

import scala.collection.JavaConverters._

case class GenProgram(universe: Universe) extends CausticBaseVisitor[String] {

  override def visitProgram(ctx: CausticParser.ProgramContext): String = {
    s"""${ if (ctx.module() != null) s"package ${ ctx.module().getText }" else "" }
       |
       |import caustic.library.collection._
       |import caustic.library.control._
       |import caustic.library.external._
       |import caustic.library.record._
       |import caustic.library.typing._
       |import caustic.library.typing.Value._
       |import caustic.runtime._
       |
       |import spray.json._
       |import DefaultJsonProtocol._
       |
       |import scala.language.implicitConversions
       |import scala.language.reflectiveCalls
       |import scala.util.Try
       |
       |${ ctx.include().asScala.map(visitInclude) mkString "\n" }
       |
       |${ ctx.declaration().asScala.map(visitDeclaration) mkString "\n" }
     """.stripMargin
  }

  override def visitInclude(ctx: CausticParser.IncludeContext): String =
    s"${ ctx.getText }._"

  override def visitStruct(ctx: CausticParser.StructContext): String =
    GenStruct(this.universe).visitStruct(ctx)

  override def visitService(ctx: CausticParser.ServiceContext): String =
    GenService(this.universe).visitService(ctx)

}
