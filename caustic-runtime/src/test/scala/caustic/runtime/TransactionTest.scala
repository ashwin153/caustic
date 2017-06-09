package caustic.runtime

import caustic.runtime._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}
import org.scalatest.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class TransactionTest extends FunSuite
  with MockitoSugar
  with Matchers {

  test("Readset is correctly generated.") {
    read(literal("A")).readset should contain theSameElementsAs Set("A")
    write(literal("A"), read(literal("B"))).readset should contain theSameElementsAs Set("B")
    read(read(literal("A"))).readset should contain theSameElementsAs Set("A")
    write(literal("A"), Literal.True).readset should have size 0
    literal("A").readset should have size 0
  }

  test("Writeset is correctly generated.") {
    write(literal("A"), Literal.True).writeset should contain theSameElementsAs Set("A")
    write(literal("A"), read(literal("B"))).writeset should contain theSameElementsAs Set("A")
    read(literal("A")).writeset should have size 0
    literal("A").writeset should have size 0
  }
  
  test("Folding is correctly performed.") {
    // Logical operations.
    runtime.equal(literal(0), literal(0.0)) shouldEqual Literal.True
    runtime.equal(literal("0"), literal("0.0")) shouldEqual Literal.True
    runtime.equal(literal(""), literal(0)) shouldEqual Literal.True
    runtime.equal(literal(0), literal("")) shouldEqual Literal.True
    runtime.not(runtime.equal(literal(0), literal(1))) shouldEqual Literal.True
    less(literal(2), literal(10)) shouldEqual Literal.True
    less(literal(-1), literal(1)) shouldEqual Literal.True
    less(literal(""), literal(1)) shouldEqual Literal.True
    less(literal(""), literal(-1)) shouldEqual Literal.False

    // Logical operations.
    and(Literal.False, Literal.True) shouldEqual Literal.False
    and(Literal.True, Literal.True) shouldEqual Literal.True
    or(Literal.False, Literal.True) shouldEqual Literal.True
    runtime.not(Literal.True) shouldEqual Literal.False

    // Arithmetic operations.
    add(literal(6), literal(9)) shouldEqual literal(15)
    sub(literal(9), literal(6)) shouldEqual literal(3)
    mul(literal(2), literal(3)) shouldEqual literal(6)
    div(literal(5), literal(2)) shouldEqual literal(2.5)
    mod(literal(5), literal(2)) shouldEqual Literal.One
    pow(literal(5), literal(2)) shouldEqual literal(25)
    log(literal(math.exp(2))) shouldEqual Literal.Two
    sin(literal(0.0)) shouldEqual Literal.Zero
    cos(literal(0.0)) shouldEqual Literal.One
    floor(literal(1.0)) shouldEqual Literal.One
    floor(literal(1.5)) shouldEqual Literal.One
    floor(literal(1.4)) shouldEqual Literal.One

    // String operations.
    runtime.length(literal("Hello")) shouldEqual literal(5.0)
    runtime.slice(literal("Hello"), literal(1), literal(3)) shouldEqual literal("el")
    runtime.concat(literal("A"), literal("bc")) shouldEqual literal("Abc")
    runtime.matches(literal("a41i3"), literal("[a-z1-4]+")) shouldEqual Literal.True
    runtime.contains(literal("abc"), literal("bc")) shouldEqual Literal.True
    runtime.contains(literal("abc"), literal("de")) shouldEqual Literal.False
  }

}
