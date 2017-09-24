package caustic.syntax

import caustic.grammar.CausticParser._
import scala.collection.JavaConverters._

/**
 * A compiler from Caustic programs into Thrift IDLs.
 */
class ThriftCompiler extends Compiler {

  override def visitProgram(ctx: ProgramContext): String = {
    val imports = ctx.module().asScala.drop(1).map(x => s"""import "${x.getText}"""").mkString("\n")
    val declarations = ctx.declaration().asScala.map(visitDeclaration)
    val structs = declarations.filter(_ startsWith "struct").mkString("\n")
    val services = declarations.filter(_ startsWith "service").mkString("\n")

    // Serialize Caustic programs as a Thrift IDL.
    s"""namespace * ${ctx.module(0).getText}
       |$imports
       |$structs
       |$services
     """.stripMargin
  }

  override def visitRecord(ctx: RecordContext): String = {
    val name = ctx.Identifier(0).getText
    val params = visitParameters(ctx.parameters())

    // Serialize records as Thrift structs.
    s"""struct $name {
       |  $params
       |}
     """.stripMargin
  }

  override def visitService(ctx: ServiceContext): String = {
    val name = ctx.Identifier(0).getText
    val functions = ctx.function().asScala.map(visitFunction)

    // Serialize services as Thrift services.
    s"""service $name {
       |$functions
       |}
     """.stripMargin
  }

  override def visitFunction(ctx: FunctionContext): String = {
    val name = ctx.Identifier().getText
    val args = visitParameters(ctx.parameters())
    val ret  = visitType(ctx.`type`())

    // Serialize functions as Thrift functions.
    s"""$ret $name(
       |  $args
       |)
     """.stripMargin
  }

  override def visitParameters(ctx: ParametersContext): String =
    ctx.parameter().asScala
      .zipWithIndex
      .map { case (p, i) => s"${i + 1}: ${visitType(p.`type`())} ${p.Identifier()}," }
      .mkString("\n")

  override def visitType(ctx: TypeContext): String =
    ctx.Identifier().getText match {
      case _ if ctx.Ampersand() != null => "string"
      case "Boolean" => "bool"
      case "Integer" => "i64"
      case "Decimal" => "double"
      case "String"  => "string"
      case _ => _
    }

}