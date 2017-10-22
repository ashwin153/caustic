package caustic.runtime.sql.dialects

import caustic.runtime.sql.{SQLDatabase, SQLDialectTest}
import java.sql.Connection
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.language.postfixOps

@RunWith(classOf[JUnitRunner])
class PostgreSQLDialectTest extends SQLDialectTest {

  override val config  =SQLDatabase.Config(
    username = "postgres",
    password = "",
    dialect = "postgresql",
    url = "jdbc:postgresql://localhost:5432/test?serverTimezone=UTC"
  )

  override def truncate(con: Connection): Unit = {
    val statement = con.createStatement()
    statement.execute("DROP TABLE IF EXISTS caustic")
    statement.close()
  }

}
