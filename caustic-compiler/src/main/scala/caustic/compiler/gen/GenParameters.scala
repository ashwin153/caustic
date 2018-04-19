package caustic.compiler.gen

import caustic.compiler.reflect._
import caustic.grammar.{CausticBaseVisitor, CausticParser}

import scala.collection.JavaConverters._

case class GenParameters(universe: Universe) extends CausticBaseVisitor[Map[String, Value]] {

  override def visitParameters(ctx: CausticParser.ParametersContext): Map[String, Value] = {
    ctx.parameter().asScala
      .map(p => p.Identifier().getText -> p.`type`())
      .toMap
      .mapValues(GenType(this.universe).visitType)
  }

}
