package caustic.beaker.storage.sql.dialects

import caustic.beaker.storage.sql.{SQLDatabase, SQLDialectTest}
import java.sql.Connection
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.language.postfixOps

@RunWith(classOf[JUnitRunner])
class MySQLDialectTest extends SQLDialectTest {

  override val config = SQLDatabase.Config(
    username = "root",
    password = "",
    dialect = "mysql",
    url = "jdbc:mysql://localhost:3306/test?serverTimezone=UTC"
  )

  override def truncate(con: Connection): Unit = {
    val statement = con.createStatement()
    statement.execute("TRUNCATE TABLE `caustic`")
    statement.close()
  }

}
