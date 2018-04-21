package caustic.compiler.gen

import caustic.grammar._

/**
 * Generates a formatted Scala comment from a comment. Caustic supports both C-style comments (/**/)
 * and C++-style comments (//). Left aligns comment strings.
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
