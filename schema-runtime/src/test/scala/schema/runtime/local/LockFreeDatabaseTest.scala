package schema.runtime.local

import org.junit.runner.RunWith
import org.scalatest.Outcome
import org.scalatest.junit.JUnitRunner
import schema.runtime.DatabaseTest

@RunWith(classOf[JUnitRunner])
class LockFreeDatabaseTest extends DatabaseTest {

  override def withFixture(test: OneArgTest): Outcome =
    test(LockFreeDatabase.empty)
  
}
