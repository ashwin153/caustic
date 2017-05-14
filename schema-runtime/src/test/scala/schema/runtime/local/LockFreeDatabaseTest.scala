package schema.runtime.local

import org.scalatest.Outcome
import schema.runtime.DatabaseTest

class LockFreeDatabaseTest extends DatabaseTest {

  override def withFixture(test: OneArgTest): Outcome =
    test(LockFreeDatabase.empty)
  
}
