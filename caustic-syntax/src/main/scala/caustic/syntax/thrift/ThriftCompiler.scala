package caustic.syntax
package thrift

import caustic.grammar.CausticParser._
import caustic.syntax.thrift.ThriftCompiler._

import org.antlr.v4.runtime.misc.ParseCancellationException
import scala.collection.JavaConverters._

/**
 * A compiler that generates a Thrift RPC interface. For some foo.acid, the ThriftCompiler generates
 * a Thrift IDL foo.thrift, and an executable FooThriftServer.scala which serves the Caustic
 * implementation when run. The ThriftCompiler gives Caustic out-of-box interoperability with a
 * variety of different programming languages and software stacks.
 *
 * @param namespace Thrift namespace.
 */
case class ThriftCompiler(namespace: String) extends Compiler {

  override def visitService(ctx: ServiceContext): String = {
    // Recurse on all declarations.
    val declarations = ctx.declaration().asScala.map(visitDeclaration)
    val structs = declarations.filter(_ startsWith "record").mkString("\n")
    val definitions = declarations.filter(_ startsWith "def").mkString("\n")

    // Construct the Thrift IDL.
    s"""namespace * ${this.namespace}
       |$structs
       |
       |service ${ctx.Identifier().getText} {
       |$definitions
       |
       |}
     """.stripMargin
  }

  override def visitDeclaration(ctx: DeclarationContext): String = {
    if (ctx.record() != null) {
      val name = ctx.record().Identifier().getText
      val params = serialize(ctx.record().parameters())

      // Serialize records as Thrift structs.
      s"""
         |struct $name {
         |$params
         |}
       """.stripMargin
    } else if (ctx.function() != null) {
      // Determine the Thrift return type of the function.
      val isref = ctx.function().Ampersand() != null
      val rtype = ctx.function().Identifier(1).getText
      val ttype = (isref, rtype) match {
        case (true, _) => "string"
        case (_, "Boolean") => "bool"
        case (_, "Integer") => "i64"
        case (_, "Decimal") => "double"
        case (_, "String") => "string"
        case (_, x) => x
      }

      // Extract the name of the function and indent its Thrift parameters.
      val name = ctx.function().Identifier(0)
      val params = serialize(ctx.function().parameters()).split("\n").map("  " + _).mkString("\n")

      // Serialize functions as Thrift functions.
      s"""
         |$ttype $name(
         |$params
         |)
       """.stripMargin
    } else {
      throw new ParseCancellationException("Unknown declaration, " + ctx.getText)
    }
  }

}

object ThriftCompiler {

  /**
   *
   * @param parameters
   * @return
   */
  def serialize(parameters: ParametersContext): String =
    parameters.parameter().asScala.zipWithIndex map { case (p, i) =>
      val name = p.Identifier()
      val kind = if (p.Ampersand() != null) "String" else p.qualifiedIdentifier().getText
      s"${ i + 1 }: $kind $name,"
    } mkString "\n"

}
