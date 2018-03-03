package caustic.beaker.storage.sql

import caustic.runtime.DatabaseTest

import org.scalatest.Outcome

import java.sql.Connection
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

trait SQLDialectTest extends DatabaseTest {

  /**
   * Returns the Travis CI database configuration.
   *
   * @return Test configuration.
   */
  def config: SQLDatabase.Config

  /**
   * Deletes all data from the database.
   *
   * @param con JDBC Connection.
   */
  def truncate(con: Connection): Unit

  override def withFixture(test: OneArgTest): Outcome = {
    // Recreate the table for each test.
    val database = SQLDatabase(this.config)

    // Wait for the database to exist, and then run the test.
    Await.ready(database.exists, 10 seconds)
    val outcome = test(database)

    // Delete all the test data.
    val connection = database.pool.getConnection()
    this.truncate(connection)
    connection.close()
    database.close()

    // Return the test outcome.
    outcome
  }

}
