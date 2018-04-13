package caustic.compiler

import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CompilerTest extends FunSuite with Matchers {

  test("Record declarations") {
    Causticc.compile {
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
    } map Scalac.compile should be a 'success
  }

  test("Service declarations") {
    Causticc.compile {
      s"""service Decrement {
         |  def apply(x: Int): Int = x - 1
         |}
       """.stripMargin
    } map Scalac.compile should be a 'success
  }

  test("Variable definitions") {
    Causticc.compile {
      s"""service Increment {
         |  def apply(x: Int): Int = {
         |    var y = x + 1
         |    y
         |  }
         |}
       """.stripMargin
    } map Scalac.compile should be a 'success
  }

  test("Conditional branching") {
    Causticc.compile {
      s"""service AbsoluteValue {
         |  def apply(x: Int): Int = if (x < 0) -x else x
         |}
       """.stripMargin
    } map Scalac.compile should be a 'success
  }

  test("Loops") {
    Causticc.compile {
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
    } map Scalac.compile should be a 'success
  }

}
