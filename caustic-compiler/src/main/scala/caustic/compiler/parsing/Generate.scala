package caustic.compiler
package parsing

import caustic.compiler.typing._
import caustic.grammar.{CausticBaseVisitor, CausticParser}

import scala.collection.JavaConverters._

/**
 * A code generator.
 *
 * @param universe Type universe.
 */
case class Generate(universe: Universe) extends CausticBaseVisitor[String] {

  override def visitProgram(ctx: CausticParser.ProgramContext): String = {
    val module = if (ctx.module() != null) s"package ${ ctx.module().getText }" else ""
    s"""$module
       |
       |import caustic.library.collection._
       |import caustic.library.control._
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

  override def visitInclude(ctx: CausticParser.IncludeContext): String = {
    // import caustic.example._
    //   => compile caustic.example
    //   => import caustic.example._
    super.visitInclude(ctx)
  }

  override def visitStruct(ctx: CausticParser.StructContext): String = {
    // struct Foo { x: Int, y: Bar, z: &Bar }
    //   => case class Foo$Internal(x: Value[Int], y: Bar$Repr, z: Reference[Bar$Repr])
    //   => case class Foo(x: scala.Int, y: Bar, z: Pointer[Bar])
    //   => object Foo {
    //        implicit def asRef(x: Foo)(implicit context: Context): Reference[Foo$] = { ... }
    //      }
    //   => import Foo._
    val struct = ctx.Identifier().getText
    val names = ctx.parameters().parameter().asScala.map(_.Identifier().getText)
    val kinds = ctx.parameters().parameter().asScala.map(p => this.universe.kind(p.`type`()))
    this.universe.bind(struct, Struct(struct, names.zip(kinds).toMap))

    s"""case class $struct$$Repr(
       |  ${ asField(ctx.parameters) }
       |)
       |
       |object $struct$$Internal {
       |
       |  implicit def asRef(x: $struct$$Internal)(
       |    implicit context: Context
       |  ): Reference[$struct$$Repr] = {
       |    val ref = Reference[$struct$$Repr](Variable.Local(context.label()))
       |    ${ names.map(f => s"ref.get('$f) := x.$f") mkString "\n    " }
       |    ref
       |  }
       |
       |}
       |
       |case class $struct$$Internal(
       |  ${ asArgument(ctx.parameters) }
       |)
       |
       |import $struct$$Internal._
       |
       |object $struct {
       |
       |  implicit def asRef(x: $struct)(implicit context: Context): Reference[$struct$$Repr] = {
       |    val ref = Reference[$struct$$Repr](Variable.Local(context.label()))
       |    ${ names.map(f => s"ref.get('$f) := x.$f") mkString "\n    " }
       |    ref
       |  }
       |
       |  implicit object $struct$$Format extends RootJsonFormat[$struct] {
       |
       |    def write(x: $struct): JsValue = JsObject(
       |      ${ names.map(f => s""""$f" -> x.$f.toJson""") mkString ", " }
       |    )
       |
       |    def read(x: JsValue): $struct = {
       |      x.asJsObject.getFields(${ names.map(f => s""""$f"""") mkString ", " }) match {
       |        case Seq(${ names mkString "," }) =>
       |          $struct(${ names.zip(kinds) map { case (n, t) => s"$n.convertTo[${ asExternal(t) }]" } mkString ", " })
       |        case _ => throw DeserializationException("$struct expected, but not found.")
       |      }
       |    }
       |
       |  }
       |
       |}
       |
       |case class $struct(
       |  ${ asExternal(ctx.parameters()) }
       |)
       |
       |import $struct._
     """.stripMargin
  }

  override def visitService(ctx: CausticParser.ServiceContext): String = {
    // service Foo { ... }
    //   => case class Foo(runtime: Runtime) { ... }
    val service = ctx.Identifier().getText
    val functions = ctx.function().asScala.map(visitFunction)
    val names = ctx.function().asScala.map(_.Identifier().getText)
    val kinds = names.map(this.universe.find(_).get.asInstanceOf[Function])
    this.universe.bind(service, Service(names.zip(kinds).toMap))

    s"""case class $service(runtime: Runtime) {
       |  ${ functions mkString "\n" }
       |}
     """.stripMargin
  }

  override def visitFunction(ctx: CausticParser.FunctionContext): String = {
    // def bar(a: &Foo): &Foo
    //   => def bar$(a: Reference[Foo$])(implicit context: Context): Reference[Foo$]
    //   => def bar(a: Pointer[Foo]): Pointer[Foo]
    //
    // def bar(a: String): Foo
    //   => def bar$(a: Value[String])(implicit context: Context): Reference[Foo$]
    //   => def bar(a: java.lang.String): Foo
    //
    // def bar(a: Foo): String
    //   => def bar$(a: Reference[Foo$])(implicit context: Context): Value[String]
    //   => def bar(a: Foo): java.lang.String
    val function = ctx.Identifier().getText
    val names = ctx.parameters().parameter().asScala.map(_.Identifier().getText)
    val kinds = ctx.parameters().parameter().asScala.map(p => this.universe.kind(p.`type`()))

    val context = this.universe.child
    names.zip(kinds) foreach { case (n, k) => context.bind(n, Variable(k)) }
    val returns = Simplify(context).visitBlock(ctx.block())
    this.universe.bind(function, Function(names.zip(kinds).toMap, returns))

    s"""def $function$$Internal(
       |  ${ asArgument(ctx.parameters) }
       |)(
       |  implicit context: Context
       |): ${ asArgument(returns.kind) } = {
       |  ${ returns.value }
       |}
       |
       |def $function(
       |  ${ asExternal(ctx.parameters()) }
       |): Try[${ asExternal(returns.kind) }] = {
       |  this.runtime execute { implicit context: Context =>
       |    Return($function$$Internal(${ names mkString ", " }).asJson)
       |  } map {
       |    case Text(x) => x
       |    case Real(x) => x.toString
       |    case Flag(x) => x.toString
       |    case Null => "null"
       |  } map {
       |    _.parseJson.convertTo[${ asExternal(returns.kind) }]
       |  }
       |}
     """.stripMargin
  }

  /**
   * Generates a record field type from the specified kind.
   *
   * @param kind Type.
   * @return Field representation.
   */
  def asField(kind: Kind): String = kind match {
    case Pointer(k: Struct) => s"Reference[${ k.name }$$Repr]"
    case k: Struct => s"${ k.name }$$Repr"
    case k: Primitive => s"${ k.name }"
  }

  def asField(ctx: CausticParser.ParameterContext): String =
    s"${ ctx.Identifier().getText }: ${ asField(this.universe.kind(ctx.`type`())) }"

  def asField(ctx: CausticParser.ParametersContext): String =
    ctx.parameter().asScala.map(asField) mkString ",\n"

  /**
   * Generates a function argument type from the specified kind.
   *
   * @param kind Type.
   * @return Argument representation.
   */
  def asArgument(kind: Kind): String = kind match {
    case Pointer(k: Struct) => s"Reference[${ k.name }$$Repr]"
    case k: Struct => s"${ k.name }$$Internal"
    case CUnit => "Unit"
    case k: Primitive => s"Value[${ k.name }]"
  }

  def asArgument(ctx: CausticParser.ParameterContext): String =
    s"${ ctx.Identifier().getText }: ${ asArgument(this.universe.kind(ctx.`type`())) }"

  def asArgument(ctx: CausticParser.ParametersContext): String =
    ctx.parameter().asScala.map(asArgument) mkString ",\n"

  /**
   * Generates an externally visible type from the specified kind.
   *
   * @param kind Type.
   * @return External representation.
   */
  def asExternal(kind: Kind): String = kind match {
    case Pointer(k: Struct) => s"${ k.name }"
    case k: Struct => k.name
    case CString => "java.lang.String"
    case CDouble => "scala.Double"
    case CInt => "scala.Int"
    case CBoolean => "scala.Boolean"
    case CUnit => "scala.Unit"
  }

  def asExternal(ctx: CausticParser.ParameterContext): String =
    s"${ ctx.Identifier().getText }: ${ asExternal(this.universe.kind(ctx.`type`())) }"

  def asExternal(ctx: CausticParser.ParametersContext): String =
    ctx.parameter().asScala.map(asExternal) mkString ",\n"

}
