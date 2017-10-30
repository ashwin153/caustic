package caustic.compiler.check

import caustic.compiler.Query
import caustic.compiler.check.visitor.DeclarationVisitor
import caustic.grammar.CausticParser
import scala.collection.JavaConverters._

/**
 * A type checker.
 */
object Check extends Query[Universe] {

  override def execute(parser: CausticParser): Universe = {
    if (parser.program().Module() != null) {
      // Scope the universe if a module is specified.
      val module = parser.program().module(0).Identifier().asScala.map(_.getText)
      val universe = Universe.root
      DeclarationVisitor(universe.child(module: _*)).visitProgram(parser.program())
      universe
    } else {
      // Otherwise, use the current universe by default.
      val universe = Universe.root
      DeclarationVisitor(universe).visitProgram(parser.program())
      universe
    }
  }

}
