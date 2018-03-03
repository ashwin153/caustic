package caustic.beaker

import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}

@RunWith(classOf[JUnitRunner])
class TransactionTest extends FunSuite with Matchers with ScalaFutures {

  test("Conflicts are correctly detected.") {
    val x = Transaction(Map("a" -> 0), Map.empty)
    val y = Transaction(Map.empty, Map("a" -> Revision(1, null)))

    // Forbid read-write conflicts.
    x.conflicts(y) shouldBe true
    y.conflicts(x) shouldBe true

    // Forbid write-write conflicts.
    y.conflicts(y) shouldBe true

    // Permit read-read conflicts.
    x.conflicts(x) shouldBe false
  }

}
