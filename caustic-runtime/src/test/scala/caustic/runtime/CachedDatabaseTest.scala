package caustic.runtime

import org.junit.runner.RunWith
import org.scalatest.Outcome
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CachedDatabaseTest extends DatabaseTest {

  override def withFixture(test: OneArgTest): Outcome =
    test(CachedDatabase.empty)

}
