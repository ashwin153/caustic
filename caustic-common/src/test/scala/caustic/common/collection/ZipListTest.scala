package caustic.common.collection

import java.util.concurrent.Executors
import org.scalatest.{FunSuite, Matchers}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class ZipListTest extends FunSuite with Matchers {

  test("toSeq should match contents") {
    ZipList.empty.toSeq shouldBe empty
    ZipList("foo", "1").toSeq should contain theSameElementsInOrderAs Seq("foo", "1")
  }

}