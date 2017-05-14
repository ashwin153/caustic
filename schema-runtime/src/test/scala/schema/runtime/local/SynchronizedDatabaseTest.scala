package schema.runtime.local

import schema.runtime.DatabaseTest
import org.scalatest.Outcome

class SynchronizedDatabaseTest extends DatabaseTest {

  override def withFixture(test: OneArgTest): Outcome =
    test(SynchronizedDatabase.empty)

}
