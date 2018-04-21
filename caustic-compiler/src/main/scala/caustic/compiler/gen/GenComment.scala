package caustic.compiler.gen

import caustic.grammar.{CausticBaseVisitor, CausticParser}

/**
 *
 */
object GenComment extends CausticBaseVisitor[String] {

  override def visitComment(ctx: CausticParser.CommentContext): String = {
    if (ctx.BlockComment() != null)
      ctx.getText.replaceAll("\n\\s*", "\n ")
    else if (ctx.LineComment() != null)
      ctx.getText.replaceAll("\n\\s*", "\n")
    else
      visitChildren(ctx)
  }

}
