package caustic.syntax.core
package visitors

import caustic.grammar.CausticBaseVisitor
import caustic.grammar.CausticParser._

/**
 *
 */
case class SymbolVisitor(
  symbols: Map[String, Symbol]
) extends CausticBaseVisitor[Symbol] {

  override def visitSymbol(ctx: SymbolContext): Symbol = {
    //
    def fold(names: List[String], sym: Symbol): Symbol = (names, sym) match {
      case (Nil, _) => sym
      case (x :: rest, Variable(_, s)) =>
        Variable(n, s) =>
      case Function(returns: Symbol, varsym: List[Variable]) extends Symbol
      case Record(p) => p.find(_.name == ctx.Identifier(0))
    }


  }
    this.symbols(ctx.Identifier(0).getText) match {
      case Variable(n, s) =>
      case Function(returns: Symbol, varsym: List[Variable]) extends Symbol
      case Record(p) => p.find(_.name == ctx.Identifier(0))
  }

}
