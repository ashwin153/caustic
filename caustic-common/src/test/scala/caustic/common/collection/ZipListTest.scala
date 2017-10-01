package caustic.common.collection

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}

@RunWith(classOf[JUnitRunner])
class ZipListTest extends FunSuite with Matchers {

  test("toSeq should match contents") {
    ZipList.empty.toSeq shouldBe empty
    ZipList("foo", "1").toSeq should contain theSameElementsInOrderAs Seq("foo", "1")
  }

}