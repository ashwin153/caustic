package caustic.mysql

import caustic.runtime._
import javax.sql.DataSource
import java.sql.Connection
import scala.collection.mutable

/**
 * A MySQL-backed database.
 *
 * @param underlying Underlying database.
 */
class MySQLDatabase private[mysql](
  underlying: DataSource
) extends RelationalDatabase(underlying) {

  def select(connection: Connection, keys: Set[Key]): Map[Key, Revision] = {
    // Load all the rows that match the keys.
    val statement = connection.prepareStatement(
      s""" SELECT `key`, `version`, `type`, `value`
         | FROM `schema`
         | WHERE `key` IN (${"?" * keys.size})
       """.stripMargin
    )

    keys.zipWithIndex.foreach { case (key, index) =>
      statement.setString(index + 1, key)
    }

    // Parse the query result set into a buffer.
    val results = statement.executeQuery()
    val buffer = mutable.Buffer.empty[(Key, Revision)]

    while (results.next()) {
      val key = results.getString("key")
      val version = results.getLong("version")

      val value = results.getInt("type") match {
        case 0 => Flag(results.getBoolean("value"))
        case 1 => Real(results.getDouble("value"))
        case 2 => Text(results.getString("value"))
      }

      buffer += (key -> Revision(version, value))
    }

    // Close the results and convert the buffer to map.
    results.close()
    statement.close()
    buffer.toMap
  }

  def upsert(connection: Connection, key: Key, revision: Revision): Unit = {
    // Prepare the SQL statement.
    val statement = connection.prepareStatement(
      s""" INSERT INTO `schema` (`key`, `version`, `type`, `value`)
         | VALUES (?, ?, ?, ?)
         | ON DUPLICATE KEY UPDATE
         | `version` = VALUES(`version`),
         | `type` = VALUES(`type`),
         | `value` = VALUES(`value`)
         """.stripMargin
    )

    // Set the values of the statement.
    statement.setString(1, key)
    statement.setLong(2, revision.version)

    revision.value match {
      case Flag(value) =>
        statement.setInt(3, 0)
        statement.setBoolean(4, value)
      case Real(value) =>
        statement.setInt(3, 1)
        statement.setDouble(4, value)
      case Text(value) =>
        statement.setInt(3, 2)
        statement.setString(4, value)
    }

    // Execute and close the statement.
    statement.executeUpdate()
    statement.close()
  }

}

object MySQLDatabase {

  /**
   * Constructs a MySQL database backed by the specified data source.
   *
   * @param source Data source.
   * @return MySQL database.
   */
  def apply(source: DataSource): MySQLDatabase = {
    // Construct the database tables if they do not already exist.
    val con = source.getConnection()
    val smt = con.createStatement()

    smt.execute(
      s""" CREATE TABLE IF NOT EXISTS `schema`(
         |   `key` varchar (200) NOT NULL,
         |   `version` BIGINT DEFAULT 0,
         |   `type` INT,
         |   `value` TEXT,
         |   PRIMARY KEY(`key`)
         | )
       """.stripMargin)

    // Construct a MySQL Database.
    con.close()
    new MySQLDatabase(source)
  }

}
