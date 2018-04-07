package caustic.library.math

import caustic.library.typing._
import caustic.library.typing.Value._

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}

@RunWith(classOf[JUnitRunner])
class PackageTest extends FunSuite with Matchers {

  test("Expressions are simplified") {
    // Absolute values.
    abs(-1) === 1 shouldBe True
    abs(+1) === 1 shouldBe True
    abs(+0) === 0 shouldBe True

    // Inverse trigonometric functions.
    asin(+0.5) >= Pi / 6 - 0.01 shouldBe True
    asin(+0.5) <= Pi / 6 + 0.01 shouldBe True
    asin(-0.5) >= - Pi / 6 - 0.01 shouldBe True
    asin(-0.5) <= - Pi / 6 + 0.01 shouldBe True

    // Rounding.
    round(0.5) === 1 shouldBe True
    round(0.4) === 0 shouldBe True
    round(0.55, 0.1) === 0.6 shouldBe True
    round(0.52, 0.1) === 0.5 shouldBe True
  }

}