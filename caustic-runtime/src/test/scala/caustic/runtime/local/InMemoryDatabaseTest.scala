package caustic.runtime
package local

import org.junit.runner.RunWith
import org.scalatest.Outcome
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class InMemoryDatabaseTest extends DatabaseTest {

  override def withFixture(test: OneArgTest): Outcome =
    test(InMemoryDatabase.empty)

}
