package caustic.beaker.storage

import org.junit.runner.RunWith
import org.scalatest.Outcome
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LocalDatabaseTest extends DatabaseTest {

  override def withFixture(test: OneArgTest): Outcome =
    test(LocalDatabase())

}