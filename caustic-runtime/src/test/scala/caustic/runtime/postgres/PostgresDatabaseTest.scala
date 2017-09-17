package caustic.runtime.postgres

import caustic.runtime.DatabaseTest
import com.mchange.v2.c3p0.ComboPooledDataSource
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, Outcome}

@RunWith(classOf[JUnitRunner])
class PostgresDatabaseTest extends DatabaseTest with BeforeAndAfterAll {

  var pool: ComboPooledDataSource = _

  override def beforeAll(): Unit = {
    this.pool = new ComboPooledDataSource()
    this.pool.setDriverClass("org.postgresql.Driver")
    this.pool.setJdbcUrl("jdbc:postgresql://localhost:5432/test?serverTimezone=UTC")
    this.pool.setUser("postgres")
    this.pool.setPassword("")
  }

  override def withFixture(test: OneArgTest): Outcome = {
    // Delete all the table metadata.
    val con = this.pool.getConnection()
    val smt = con.createStatement()
    smt.execute("DROP TABLE IF EXISTS schema")
    con.close()

    // Run the tests.
    val database = PostgreSQLDatabase(this.pool)
    test(database)
  }

  override def afterAll(): Unit = {
    this.pool.close()
  }

}
