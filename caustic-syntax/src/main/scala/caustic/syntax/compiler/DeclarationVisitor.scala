package caustic.syntax
package compiler

import caustic.grammar.CausticBaseVisitor
import caustic.grammar.CausticParser._
import caustic.runtime.{Flag, Real, Text, Transaction, thrift}
import org.apache.thrift.TException
import org.apache.thrift.async.AsyncMethodCallback
import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.util.{Failure, Success}

/**
 *
 */
class DeclarationVisitor(
  symbols: Map[String, Symbol],
  namespace: String
) extends CausticBaseVisitor[String] {

  // Local symbol table.
  val locals: mutable.Map[String, Symbol] = mutable.Map.empty

  override def visitDeclaration(ctx: DeclarationContext): String =
    super.visitDeclaration(ctx)

  override def visitRecord(ctx: RecordContext): String = {
    // Extract the service signature.
    val name = ctx.Identifier().getText
    val args = visitParameters(ctx.parameters())

    // Add the record to the symbol table.

    // Do not serialize the record.

  }

  override def visitService(ctx: ServiceContext): String = {
    // Extract the service signature.
    val name = ctx.Identifier()
    val functions = ctx.function().asScala.map(visitFunction).mkString("\n")

    // Add the service to the symbol table.

    // Serialize the service.
    s"""case class $name(database: thrift.Database.Client) extends thrift.$name {
       |$functions
       |}
     """.stripMargin
  }

  override def visitFunction(ctx: FunctionContext): String = {
    // Extract the function signature.
    val name = ctx.Identifier().getText
    val args = visitParameters(ctx.parameters())
    val ret  = visitType(ctx.`type`())

    // Visit the body of the function.
    val body = BlockVisitor(this.symbols ++ this.locals, this.namespace + "$" + name)
      .visitBlock(ctx.block())

    // Add the function to the symbol table.

    // Parse the output of the body into the return value.
    val result =

    // Convert the function into executable Scala.
    s"""  override def $name($args, res: AsyncMethodCallback[$ret]): Unit = {
       |    this.database.execute($body).onComplete {
       |      case Success(_) => res.onComplete($result)
       |      case Failure(e: TException) => res.onError(e)
       |      case Failure(e) => res.onError(new thrift.ExecutionException()
       |    }
       |  }
     """.stripMargin
  }


  override def visitParameters(ctx: ParametersContext): String =
    ctx.parameter().asScala.map(visitParameter).mkString(", ")

  override def visitParameter(ctx: ParameterContext): String =
    super.visitParameter(ctx)

  override def visitType(ctx: TypeContext): String =
    super.visitType(ctx)

}
