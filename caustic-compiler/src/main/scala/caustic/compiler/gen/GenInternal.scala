package caustic.compiler.gen

import caustic.compiler.Error
import caustic.compiler.reflect._
import caustic.grammar.{CausticBaseVisitor, CausticParser}

import scala.collection.JavaConverters._

case class GenInternal(universe: Universe) extends CausticBaseVisitor[String] {

  override def visitStruct(ctx: CausticParser.StructContext): String = {
    val name = ctx.Identifier().getText
    val fields = ctx.parameters().parameter().asScala.map(_.Identifier().getText)

    s"""object $name$$Internal {
       |
       |  implicit def asRef(x: $name$$Internal)(
       |    implicit context: Context
       |  ): Reference[$name$$Repr] = {
       |    val ref = Reference[$name$$Repr](Variable.Local(context.label()))
       |    ${ fields.map(f => s"ref.get('$f) := x.$f") mkString "\n    " }
       |    ref
       |  }
       |
       |}
       |
       |import $name$$Internal._
       |
       |case class $name$$Internal(
       |  ${ visitParameters(ctx.parameters()) }
       |)
     """.stripMargin
  }

  override def visitFunction(ctx: CausticParser.FunctionContext): String = {
    val name = ctx.Identifier().getText
    val args = GenParameters(this.universe).visitParameters(ctx.parameters())
    val body = this.universe.child
    args foreach { case (n, t) => body.bind(n, Variable(t)) }

    s"""def $name$$Internal(
       |  ${ visitParameters(ctx.parameters()) }
       |)(
       |  implicit context: Context
       |): ${ visitType(ctx.`type`()) } = {
       |  ${ GenBlock(body).visitBlock(ctx.block()).value }
       |}
     """.stripMargin
  }

  override def visitParameters(ctx: CausticParser.ParametersContext): String =
    ctx.parameter().asScala.map(visitParameter).mkString(",\n  ")

  override def visitParameter(ctx: CausticParser.ParameterContext): String =
    s"${ ctx.Identifier().getText }: ${ visitType(ctx.`type`()) }"

  override def visitType(ctx: CausticParser.TypeContext): String =
    this.universe.find(ctx.getText.replaceAll("\\s", "")) match {
      case Some(_: Pointer) => s"Reference[${ ctx.getText.dropRight(1) }$$Repr]"
      case Some(_: Defined) => s"Reference[${ ctx.getText }$$Repr]"
      case Some(CString) => "Value[String]"
      case Some(CDouble) => "Value[Double]"
      case Some(CInt) => "Value[Int]"
      case Some(CBoolean) => "Value[Boolean]"
      case Some(CUnit) => "Unit"
      case Some(any) => throw Error.Type(s"Field cannot be of type $any.", Error.Trace(ctx))
      case None => throw Error.Type(s"Unknown type ${ ctx.getText }.", Error.Trace(ctx))
    }

}
