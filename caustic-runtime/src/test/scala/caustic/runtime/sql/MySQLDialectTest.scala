package caustic.runtime
package sql

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, Outcome}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

@RunWith(classOf[JUnitRunner])
class MySQLDialectTest extends DatabaseTest with BeforeAndAfterAll {

  var database: SQLDatabase = _

  override def beforeAll(): Unit =
    this.database = SQLDatabase(
      username = "root",
      password = "",
      dialect = "mysql",
      url = "jdbc:mysql://localhost:3306/test?serverTimezone=UTC"
    )

  override def afterAll(): Unit =
    this.database.close()

  override def withFixture(test: OneArgTest): Outcome = {
    // Recreate the table for each test..
    val connection = this.database.underlying.getConnection()
    connection.createStatement().execute("DROP TABLE IF EXISTS `caustic`")
    connection.close()

    // Run the tests.
    Await.result(this.database.exists, 10 seconds)
    test(this.database)
  }

}
