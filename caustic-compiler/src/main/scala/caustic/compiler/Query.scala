package caustic.compiler

import caustic.grammar._
import org.antlr.v4.runtime.{CharStreams, CommonTokenStream}
import scala.collection.mutable

/**
 *
 */
trait Query[T] {

  /**
   *
   * @param parser
   * @return
   */
  def execute(parser: CausticParser): T

  /**
   *
   * @return
   */
  def execute: String => T = new mutable.HashMap[String, T]() {
    override def apply(source: String): T = getOrElseUpdate(source, {
      val lexer = new CausticLexer(CharStreams.fromString(source))
      val tokens = new CommonTokenStream(lexer)
      execute(new CausticParser(tokens))
    })
  }

}
