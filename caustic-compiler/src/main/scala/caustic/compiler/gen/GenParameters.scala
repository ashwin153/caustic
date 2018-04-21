package caustic.compiler.gen

import caustic.compiler.reflect._
import caustic.grammar._

import scala.collection.JavaConverters._

/**
 * Generates statically-typed symbol tables from parameters.
 *
 * @param universe Type universe.
 */
case class GenParameters(universe: Universe) extends CausticBaseVisitor[Map[String, Simple]] {

  override def visitParameters(ctx: CausticParser.ParametersContext): Map[String, Simple] = {
    ctx.parameter().asScala
      .map(p => p.Identifier().getText -> p.`type`())
      .toMap
      .mapValues(GenType(this.universe).visitType)
  }

}
