package caustic.compiler.gen

import caustic.compiler.reflect._
import caustic.grammar.{CausticBaseVisitor, CausticParser}

import scala.collection.JavaConverters._

case class GenService(universe: Universe) extends CausticBaseVisitor[String] {

  override def visitService(ctx: CausticParser.ServiceContext): String = {
    val name = ctx.Identifier().getText
    val functions = ctx.function().asScala.map(GenFunction(this.universe).visitFunction)
    this.universe.bind(name, Service(functions.toMap))

    s"""object $name {
       |  ${ ctx.function().asScala.map(GenInternal(this.universe.child).visitFunction).mkString("\n") }
       |}
       |
       |import $name._
       |
       |case class $name(runtime: Runtime) {
       |  ${ ctx.function().asScala.map(GenExternal(this.universe).visitFunction).mkString("\n") }
       |}
     """.stripMargin
  }

}
