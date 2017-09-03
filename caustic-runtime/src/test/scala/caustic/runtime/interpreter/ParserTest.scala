package caustic.runtime.interpreter

import caustic.runtime.interpreter
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}

@RunWith(classOf[JUnitRunner])
class ParserTest extends FunSuite with Matchers {

  test("Symbols are cached.") {
    flag(true) should be theSameInstanceAs flag(true)
    flag(false) should be theSameInstanceAs flag(false)
    text("") should be theSameInstanceAs text("")
    real(0) should be theSameInstanceAs real(0)
    real(1) should be theSameInstanceAs real(1)
  }

  test("Operations are simplified.") {
    // Math Operations.
    add(real(6), real(9)) shouldEqual real(15)
    add(text("a"), text("b")) shouldEqual text("ab")
    add(flag(true), flag(false)) shouldEqual text("truefalse")
    add(text("a"), real(0)) shouldEqual text("a0.0")
    add(text("a"), flag(true)) shouldEqual text("atrue")
    add(real(0), flag(true)) shouldEqual text("0.0true")
    sub(real(9), real(6)) shouldEqual real(3)
    mul(real(2), real(3)) shouldEqual real(6)
    div(real(5), real(2)) shouldEqual real(2.5)
    mod(real(5), real(2)) shouldEqual real(1)
    pow(real(5), real(2)) shouldEqual real(25)
    log(real(math.exp(2))) shouldEqual real(2)
    sin(real(0.0)) shouldEqual real(0)
    cos(real(0.0)) shouldEqual real(1)
    floor(real(1.0)) shouldEqual real(1)
    floor(real(1.5)) shouldEqual real(1)
    floor(real(1.4)) shouldEqual real(1)

    // Logical Operations.
    both(flag(false), flag(true)) shouldEqual flag(false)
    both(flag(true), flag(true)) shouldEqual flag(true)
    either(flag(true), flag(false)) shouldEqual flag(true)
    either(flag(false), flag(false)) shouldEqual flag(false)
    negate(flag(false)) shouldEqual flag(true)
    negate(real(0)) shouldEqual flag(true)
    negate(real(1)) shouldEqual flag(false)
    negate(text("")) shouldEqual flag(true)
    negate(text("foo")) shouldEqual flag(false)

    // String Operations.
    interpreter.length(text("Hello")) shouldEqual real(5.0)
    slice(text("Hello"), real(1), real(3)) shouldEqual text("el")
    matches(text("a41i3"), text("[a-z1-4]+")) shouldEqual flag(true)
    matches(text("a41i3"), text("[a-z1-4]")) shouldEqual flag(false)
    contains(text("abc"), text("bc")) shouldEqual flag(true)
    contains(text("abc"), text("de")) shouldEqual flag(false)
    indexOf(text("Hello"), text("l")) shouldEqual real(2)

    // Comparison Operations.
    interpreter.equal(real(0), real(0.0)) shouldEqual flag(true)
    interpreter.equal(text("a"), text("a")) shouldEqual flag(true)
    interpreter.equal(text(""), real(0)) shouldEqual flag(false)
    interpreter.equal(flag(true), flag(false)) shouldEqual flag(false)
    less(real(2), real(10)) shouldEqual flag(true)
    less(real(-1), real(1)) shouldEqual flag(true)
    less(text("a"), text("ab")) shouldEqual flag(true)
    less(flag(false), flag(true)) shouldEqual flag(true)
  }

}
