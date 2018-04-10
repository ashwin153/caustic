package caustic.compiler

import caustic.compiler.parsing.Generate
import caustic.compiler.typing.Universe
import caustic.example.{Counter, Total}
//import caustic.example.Counter
//import caustic.example.{Counter, Total$}
import caustic.grammar.{CausticLexer, CausticParser}
import caustic.runtime.{Runtime, Volume}
import org.antlr.v4.runtime.{CharStreams, CommonTokenStream}
import org.scalatest.FunSuite

class CompilerTest extends FunSuite {

  /**
   *
   * @param source
   * @return
   */
  def compile(source: String): String = {
    val lexer = new CausticLexer(CharStreams.fromString(source))
    val tokens = new CommonTokenStream(lexer)
    Generate(Universe.root).visitProgram(new CausticParser(tokens).program())
  }

  test("Simplify") {
    println(compile(
      s"""module caustic.example
         |
         |struct Total {
         |  count: Int
         |}
         |
         |service Counter {
         |
         |  def increment(x: Total&): Int = {
         |    let x = 4
         |    val x = 3
         |
         |    if (x.count == null) x.count = 1 else x.count += 1
         |    x.count
         |  }
         |
         |  def incrementTwice(x: Total&): Total& = {
         |    increment(x)
         |    increment(x)
         |    x
         |  }
         |}
      """.stripMargin
    ))

    val runtime = Runtime(Volume.Memory())
    println(Counter(runtime).incrementTwice(Total(2)))
  }

}
