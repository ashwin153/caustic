package schema.runtime.local

import org.junit.runner.RunWith
import schema.runtime.DatabaseTest
import org.scalatest.Outcome
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SynchronizedDatabaseTest extends DatabaseTest {

  override def withFixture(test: OneArgTest): Outcome =
    test(SynchronizedDatabase.empty)

}
