package caustic.compiler.gen

import caustic.compiler.Error
import caustic.compiler.reflect._
import caustic.grammar.{CausticBaseVisitor, CausticParser}

import scala.collection.JavaConverters._

case class GenInternal(universe: Universe) extends CausticBaseVisitor[String] {

  override def visitService(ctx: CausticParser.ServiceContext): String = {
    val name = ctx.Identifier().getText

    s"""object $name {
       |
       |  ${ ctx.function().asScala.map(GenInternal(this.universe.child).visitFunction).mkString("\n") }
       |
       |}
       |
       |import $name._
     """.stripMargin
  }

  override def visitStruct(ctx: CausticParser.StructContext): String = {
    val name = ctx.Identifier().getText
    val params = ctx.parameters().parameter().asScala.map(_.Identifier().getText)
    val fields = GenParameters(this.universe).visitParameters(ctx.parameters())
    val struct = Defined(fields)

    this.universe.bind(name, struct)
    this.universe.bind(s"$name$$Constructor", Function(s"$name$$Internal", fields.values.toList, struct))
    this.universe.bind(s"$name&", Pointer(struct))
    this.universe.bind(s"$name&$$Constructor", Function(s"Reference.Remote[$name$$Internal]", List(CString), Pointer(struct)))

    s"""object $name$$Internal {
       |
       |  implicit def reference(x: $name$$Internal)(
       |    implicit context: Context
       |  ): Reference[$name$$Internal] = {
       |    val ref = Reference.Local[$name$$Internal](context.label())
       |    ${ params.map(f => s"ref.get('$f) := x.$f") mkString "\n    " }
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
    val returns = GenType(this.universe).visitType(ctx.`type`())
    this.universe.bind(name, Function(s"$name$$Internal", args.values.toList, returns))

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
      case Some(_: Pointer) => s"Reference[${ ctx.getText.dropRight(1) }$$Internal]"
      case Some(_: Defined) => s"Reference[${ ctx.getText }$$Internal]"
      case Some(CList(x)) => s"List[$x]"
      case Some(CSet(x)) => s"Set[$x]"
      case Some(CMap(k, v)) => s"Map[$k, $v]"
      case Some(CString) => "Value[String]"
      case Some(CDouble) => "Value[Double]"
      case Some(CInt) => "Value[Int]"
      case Some(CBoolean) => "Value[Boolean]"
      case Some(CUnit) => "Unit"
      case Some(any) => throw Error.Type(s"Field cannot be of type $any.", Error.Trace(ctx))
      case None => throw Error.Type(s"Unknown type ${ ctx.getText }.", Error.Trace(ctx))
    }

}
