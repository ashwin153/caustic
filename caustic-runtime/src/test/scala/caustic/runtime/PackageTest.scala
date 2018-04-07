package caustic.runtime

import caustic.runtime

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}

@RunWith(classOf[JUnitRunner])
class PackageTest extends FunSuite with Matchers {

  test("Literals are cached") {
    True should be theSameInstanceAs True
    False should be theSameInstanceAs False
    text("") should be theSameInstanceAs Empty
  }

  test("Expressions are simplified") {
    // Math Expressions.
    add(6, 9) shouldEqual real(15)
    add("a", "b") shouldEqual text("ab")
    add(true, false) shouldEqual real(1)
    add("a", 0) shouldEqual text("a0")
    add("a", true) shouldEqual text("atrue")
    add(3.2, true) shouldEqual real(4.2)
    sub(9, 6) shouldEqual real(3)
    mul(2, 3) shouldEqual real(6)
    div(5, 2) shouldEqual real(2.5)
    mod(5, 2) shouldEqual real(1)
    pow(5, 2) shouldEqual real(25)
    log(math.exp(2)) shouldEqual real(2)
    sin(0) shouldEqual real(0)
    cos(0) shouldEqual real(1)
    floor(1) shouldEqual real(1)
    floor(1.5) shouldEqual real(1)

    // String Expressions.
    runtime.length("hello") shouldEqual real(5.0)
    slice("hello", 1, 3) shouldEqual text("el")
    matches("a41i3", "[a-z1-4]+") shouldEqual True
    matches("a41i3", "[a-z1-4]") shouldEqual False
    contains("abc", "bc") shouldEqual True
    contains("abc", "de") shouldEqual False
    indexOf("hello", "l") shouldEqual real(2)

    // Logical Expressions.
    both(False, True) shouldEqual False
    both(True, True) shouldEqual True
    either(True, False) shouldEqual True
    either(False, False) shouldEqual False
    negate(False) shouldEqual True
    negate(0) shouldEqual True
    negate(1) shouldEqual False
    negate("") shouldEqual True
    negate("foo") shouldEqual False
    runtime.equal(Null, Null) shouldEqual True
    runtime.equal(Null, 0) shouldEqual True
    runtime.equal(0, 0) shouldEqual True
    runtime.equal("a", "a") shouldEqual True
    runtime.equal("", 0) shouldEqual False
    runtime.equal("0", 0) shouldEqual True
    runtime.equal("0.0", 0) shouldEqual False
    runtime.equal(True, False) shouldEqual False
    less(2, 10) shouldEqual True
    less(-1, 1) shouldEqual True
    less("a", "ab") shouldEqual True
    less(False, True) shouldEqual True
  }

}