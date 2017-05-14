package schema.mysql

import org.scalatest.Outcome
import schema.runtime.DatabaseTest

class MySqlDatabaseTest extends DatabaseTest {

  override def withFixture(test: OneArgTest): Outcome = {
    val database = MySqlDatabase("localhost", 3306, "root", "", test = true)
    try {
      test(database)
    } finally {
      database.close()
    }
  }

}