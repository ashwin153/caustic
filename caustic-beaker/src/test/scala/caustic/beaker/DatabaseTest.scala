package caustic.beaker

import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, Outcome, fixture}
import scala.util.{Failure, Success}

/**
 *
 */
@RunWith(classOf[JUnitRunner])
trait DatabaseTest extends fixture.FunSuite
  with ScalaFutures
  with MockitoSugar
  with Matchers {

  type FixtureParam = Database

  implicit val defaultPatience: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(100, Millis)
  )

  /**
   *
   * @param test
   * @return
   */
  def withFixture(test: OneArgTest): Outcome

  test("Transactions are monotonic.") { db =>
    db.commit(Transaction(Map.empty, Map("k" -> Revision(1, Array.empty))))
    db.commit(Transaction(Map.empty, Map("k" -> Revision(0, Array.empty))))
    db.read(Set("k")) shouldEqual Success(Map("k" -> Revision(1, Array.empty)))
  }

  test("Transactions detect conflicts.") { db =>
    val x = Transaction(Map.empty, Map("k" -> Revision(1, Array.empty)))
    val y = Transaction(Map("k" -> 0L), Map.empty)
    db.commit(x) shouldBe Success(())
    db.commit(y) shouldBe Failure(Database.Conflicts(Map("k" -> Revision(1, Array.empty))))
  }

}