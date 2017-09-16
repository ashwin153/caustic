package caustic.syntax.scala

import caustic.grammar.CausticParser
import caustic.grammar.CausticParser.DeclarationContext
import caustic.syntax.Compiler

import org.antlr.v4.runtime.misc.ParseCancellationException

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 *
 */
case class ScalaCompiler(
  symbols: mutable.Map[String, Type]
) extends Compiler {

  override def visitProgram(ctx: CausticParser.ProgramContext): String = {
    // Extract the module.
    val module = ctx.module(0).getText

    // Extract the imports, and compile the files.
    val imports = ctx.module().asScala.drop(1)
      .map(m => s"""import ${m.getText}""")
      .mkString("\n")

    // Extract the declarations.
    val declarations = ctx.declaration().asScala
      .map(visitDeclaration)
      .mkString("\n")

    // Serialize the module and imports.
    s"""package $module
       |$imports
       |$declarations
     """.stripMargin
  }

  override def visitDeclaration(ctx: DeclarationContext): String =
    super.visitChildren(ctx)

  override def visitService(ctx: CausticParser.ServiceContext): String =
    super.visitChildren(ctx)

  override def visitFunction(ctx: CausticParser.FunctionContext): String = {
    // Extract the function name.
    val name = ctx.Identifier(0).getText

    // Extract the function arguments.
    val args = ctx.parameters().parameter().asScala
      .map(x => (x.Identifier(0).getText, x.Identifier(1).getText))
      .map(x => Parameter(x._1, this.symbols(x._2)))

    // Extract the function return value.
    val returns = ctx.Identifier(1).getText

    // Insert the record declaration into the symbol table.
    this.symbols += ctx.Identifier(0).getText -> Function(this.symbols(returns), args.toList)

    s"""def $name(${visitParameters(ctx.parameters())}): $returns = {
       |
       |}
     """.stripMargin
  }

  override def visitRecord(ctx: CausticParser.RecordContext): String = {
    // Extract the record name and fields.
    val name = ctx.Identifier(0).getText
    val fields = ctx.parameters().parameter().asScala
      .map(x => (x.Identifier(0).getText, x.Identifier(1).getText))
      .map(x => Parameter(x._1, this.symbols(x._2)))

    // If the record extends another record, then add the fields of the inherited record.
    if (ctx.Extends() != null) {
      this.symbols(ctx.Identifier(1).getText) match {
        case Record(parent) => fields ++= parent
        case _ => throw new ParseCancellationException("Records can only inherit from records.")
      }
    }

    // Insert the record declaration into the symbol table.
    this.symbols += name -> Record(fields.toList)

    // Serialize the record as a case class.
    s"""case class $name(${visitParameters(ctx.parameters())})"""
  }

  override def visitParameters(ctx: CausticParser.ParametersContext): String =
    // Serialize parameters as a new-line delimited list. (x: Foo, y: Bar)
    ctx.parameter().asScala
      .map(x => s"""${x.Identifier(0).getText}: ${x.Identifier(1).getText}""")
      .mkString(",\n")

}
