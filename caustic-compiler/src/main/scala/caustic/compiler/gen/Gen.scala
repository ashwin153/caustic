package caustic.compiler.gen

import caustic.compiler.reflect._
import caustic.grammar._

import scala.collection.JavaConverters._

/**
 * Generates a Scala program from a program.
 *
 * @param universe Type universe.
 */
case class Gen(universe: Universe) extends CausticBaseVisitor[String] {

  override def visitProgram(ctx: CausticParser.ProgramContext): String = {
    s"""${ if (ctx.module() != null) s"package ${ ctx.module().Identifier().asScala.mkString(".") }" else ""  }
       |
       |import caustic.library._
       |import caustic.library.control._
       |import caustic.library.typing._
       |import caustic.library.typing.collection._
       |import caustic.library.typing.record._
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
    s"""${ GenInternal(this.universe).visitStruct(ctx) }
       |${ GenExternal(this.universe).visitStruct(ctx) }
     """.stripMargin

  override def visitService(ctx: CausticParser.ServiceContext): String =
    s"""${ GenInternal(this.universe).visitService(ctx) }
       |${ GenExternal(this.universe).visitService(ctx) }
     """.stripMargin

}
