package schema.mysql

import com.mchange.v2.c3p0.ComboPooledDataSource
import org.scalatest.{BeforeAndAfterAll, Outcome}
import schema.runtime.DatabaseTest

class MySQLDatabaseTest extends DatabaseTest with BeforeAndAfterAll {

  var pool: ComboPooledDataSource = _

  override def beforeAll(): Unit = {
    this.pool = new ComboPooledDataSource()
    this.pool.setDriverClass("com.mysql.cj.jdbc.Driver")
    this.pool.setJdbcUrl(s"jdbc:mysql://localhost:3306/schema?serverTimezone=UTC")
    this.pool.setUser("root")
    this.pool.setPassword("")
  }

  override def withFixture(test: OneArgTest): Outcome = {
    // Delete all the table metadata.
    val con = this.pool.getConnection()
    val smt = con.createStatement()
    smt.execute("DROP TABLE IF EXISTS `schema`.`schema`")
    con.close()

    // Run the tests.
    val database = MySQLDatabase(this.pool)
    test(database)
  }

  override def afterAll(): Unit = {
    this.pool.close()
  }

}