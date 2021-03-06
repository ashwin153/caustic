package caustic.library.typing.record

import caustic.library.typing.{Value, _}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}

@RunWith(classOf[JUnitRunner])
class ReferenceTest extends FunSuite with Matchers {

  case class Foo(
    x: Value[Int],
    y: Reference[Bar],
    z: Bar
  )

  case class Bar(
    a: String
  )

  test("References are statically typed") {
    val foo = Reference[Foo](Variable.Local("foo"))
    "foo.get('a)" shouldNot compile
    "foo.get('x)" should compile
  }

}
