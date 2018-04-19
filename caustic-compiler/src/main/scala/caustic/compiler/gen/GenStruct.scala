package caustic.compiler.gen

import caustic.compiler.Error
import caustic.compiler.reflect._
import caustic.grammar.{CausticBaseVisitor, CausticParser}
import scala.collection.JavaConverters._

case class GenStruct(universe: Universe) extends CausticBaseVisitor[String] {

  override def visitStruct(ctx: CausticParser.StructContext): String = {
    val name = ctx.Identifier().getText
    val fields = GenParameters(this.universe).visitParameters(ctx.parameters())
    val struct = Defined(fields)

    // Bind the struct and its various constructors to the universe.
    this.universe.bind(name, struct)
    this.universe.bind(s"$name$$Constructor", Function(s"$name$$Internal", fields.values.toList, struct))
    this.universe.bind(s"$name&", Pointer(struct))
    this.universe.bind(s"$name&$$Constructor", Function(s"Reference.Remote[$name$$Repr]", List(CString), Pointer(struct)))

    s"""case class $name$$Repr(
       | ${ visitParameters(ctx.parameters()) }
       |)
       |
       |${ GenInternal(this.universe).visitStruct(ctx) }
       |${ GenExternal(this.universe).visitStruct(ctx) }
     """.stripMargin
  }

  override def visitParameters(ctx: CausticParser.ParametersContext): String =
    ctx.parameter().asScala.map(visitParameter).mkString(",\n  ")

  override def visitParameter(ctx: CausticParser.ParameterContext): String =
    s"${ ctx.Identifier().getText }: ${ visitType(ctx.`type`()) }"

  override def visitType(ctx: CausticParser.TypeContext): String =
    this.universe.find(ctx.getText.replaceAll("\\s", "")) match {
      case Some(Pointer(_: Defined)) => s"Reference[${ ctx.getText.dropRight(1) }$$Repr]"
      case Some(_: Defined) => s"${ ctx.getText }$$Repr"
      case Some(CList(x)) => s"List[$x]"
      case Some(CSet(x)) => s"Set[$x]"
      case Some(CMap(k, v)) => s"Map[$k, $v]"
      case Some(CString) => "String"
      case Some(CDouble) => "Double"
      case Some(CInt) => "Int"
      case Some(CBoolean) => "Boolean"
      case Some(CUnit) => "Unit"
      case Some(any) => throw Error.Type(s"Field cannot be of type $any.", Error.Trace(ctx))
      case None => throw Error.Type(s"Unknown type ${ ctx.getText }.", Error.Trace(ctx))
    }

}
