package caustic.compiler

import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import scala.reflect.runtime._
import scala.tools.reflect.ToolBox

@RunWith(classOf[JUnitRunner])
class CausticcTest extends FunSuite with Matchers {

  /**
   * A compiler for the Scala programming language.
   */
  def Scalac(source: String): Any = {
    val toolbox = currentMirror.mkToolBox()
    toolbox.eval(toolbox.parse(source))
  }

  test("Record declarations") {
    Causticc {
      s"""struct Bar {
         |  a: String
         |}
         |
         |struct Foo {
         |  b: Int,
         |  c: Bar&,
         |  d: Bar
         |}
       """.stripMargin
    } map Scalac should be a 'success
  }

  test("Service declarations") {
    Causticc {
      s"""service Decrement {
         |  def apply(x: Int): Int = x - 1
         |}
       """.stripMargin
    } map Scalac should be a 'success
  }

  test("Variable definitions") {
    Causticc {
      s"""service Increment {
         |  def apply(x: Int): Int = {
         |    var y = x + 1
         |    y
         |  }
         |}
       """.stripMargin
    } map Scalac should be a 'success
  }

  test("Conditional branching") {
    Causticc {
      s"""service AbsoluteValue {
         |  def apply(x: Int): Int = if (x < 0) -x else x
         |}
       """.stripMargin
    } map Scalac should be a 'success
  }

  test("Loops") {
    Causticc {
      s"""service Factorial {
         |  def apply(x: Int): Int = {
         |    var factorial = 1
         |    var y = x
         |
         |    while (y >= 2) {
         |      factorial *= y
         |      y -= 1
         |    }
         |
         |    factorial
         |  }
         |}
       """.stripMargin
    } map Scalac should be a 'success
  }

}
