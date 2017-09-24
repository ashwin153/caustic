package caustic.runtime
package relational

import java.sql.Connection
import javax.sql.DataSource
import scala.collection.mutable
import scala.concurrent.ExecutionContext

/**
 * A MySQL backed database.
 *
 * @param underlying Underlying MySQL DataStore.
 * @param ec Implicit execution context.
 */
case class MySQLDatabase(underlying: DataSource)(
  implicit ec: ExecutionContext
) extends RelationalDatabase(underlying) {

  override def schema: String =
    s""" CREATE TABLE IF NOT EXISTS `caustic`(
       |   `key` varchar (200) NOT NULL,
       |   `version` BIGINT DEFAULT 0,
       |   `type` INT,
       |   `value` TEXT,
       |   PRIMARY KEY(`key`)
       | )
     """.stripMargin

  def select(connection: Connection, keys: Set[Key]): Map[Key, Revision] = {
    // Load all the rows that match the keys.
    val statement = connection.prepareStatement(
      s""" SELECT `key`, `version`, `type`, `value`
         | FROM `caustic`
         | WHERE `key` IN (${List.fill(keys.size)("?").mkString(",")})
       """.stripMargin
    )

    keys.zipWithIndex.foreach {
      case (key, index) => statement.setString(index + 1, key)
    }

    // Parse the query result set into a buffer.
    val results = statement.executeQuery()
    val buffer = mutable.Buffer.empty[(Key, Revision)]

    while (results.next()) {
      val key = results.getString("key")
      val version = results.getLong("version")

      val value = results.getInt("type") match {
        case 0 => flag(results.getBoolean("value"))
        case 1 => real(results.getDouble("value"))
        case 2 => text(results.getString("value"))
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
      s""" INSERT INTO `caustic` (`key`, `version`, `type`, `value`)
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
