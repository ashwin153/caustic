package caustic.runtime.sql
package dialects

import caustic.runtime._

import java.sql.Connection
import scala.collection.mutable

object MySQLDialect extends SQLDialect {

  /**
   * A type encoding. Literals are stored in a heterogeneous column, and their type is inferred from
   * the value of the corresponding type column. Because types are stored in a fixed-width column,
   * they must be a single character in length.
   */
  object Type {
    val Flag = "F"
    val Real = "R"
    val Text = "T"
    val None = "N"
  }

  override val driver: String = "com.mysql.cj.jdbc.Driver"

  override def create(con: Connection): Unit = {
    // MySQL limits the size of the maximum key length to 767 bytes. Because it uses UTF-8 encoding
    // which requires up to 3 bytes per character, the key may be up to 255 characters in length.
    // https://wildlyinaccurate.com/mysql-specified-key-was-too-long-max-key-length-is-767-bytes/
    val sql =
      s""" CREATE TABLE IF NOT EXISTS `caustic` (
         |   `key` VARCHAR(255) NOT NULL,
         |   `version` BIGINT,
         |   `type` CHAR(1),
         |   `value` TEXT,
         |   PRIMARY KEY(`key`)
         |  )
       """.stripMargin

    val statement = con.createStatement()
    statement.execute(sql)
    statement.close()
  }

  override def select(con: Connection, keys: Set[Key]): Map[Key, Revision] = {
    // Benchmarks show that WHERE IN is sufficiently performant even for large numbers of values.
    // Alternative approaches might be to construct and join a temporary table or to issue separate
    // SELECT queries for each key, but both were shown to be equally or less performant.
    // https://stackoverflow.com/q/4514697/1447029
    val sql =
      s""" SELECT `key`, `version`, `type`, `value`
         | FROM `caustic`
         | WHERE `key` IN (${List.fill(keys.size)("?").mkString(",")})
       """.stripMargin

    // Execute the statement, and parse the returned ResultSet.
    val statement = con.prepareStatement(sql)
    keys.zipWithIndex foreach { case (k, i) => statement.setString(i + 1, k) }
    val resultSet = statement.executeQuery()

    val buffer = mutable.Buffer.empty[(Key, Revision)]
    while (resultSet.next()) {
      val value = resultSet.getString("type") match {
        case Type.Flag => flag(resultSet.getBoolean("value"))
        case Type.Real => real(resultSet.getDouble("value"))
        case Type.Text => text(resultSet.getString("value"))
        case Type.None => None
      }

      buffer += (resultSet.getString("key") -> Revision(resultSet.getLong("version"), value))
    }

    resultSet.close()
    statement.close()
    buffer.toMap
  }

  override def upsert(con: Connection, changes: Map[Key, Revision]): Unit = {
    // Benchmarks show that this query performs significantly better than a REPLACE INTO and
    // equivalently to an INSERT in the absence of conflicts.
    val sql =
      s""" INSERT INTO `caustic` (`key`, `version`, `type`, `value`)
         | VALUES ${Seq.fill(changes.size)("(?, ?, ?, ?)").mkString(",")}
         | ON DUPLICATE KEY UPDATE
         | `version` = VALUES(`version`),
         | `type` = VALUES(`type`),
         | `value` = VALUES(`value`)
       """.stripMargin

    // Execute the statement and return.
    val statement = con.prepareStatement(sql)
    changes.zipWithIndex foreach { case ((k, r), i) =>
      val row = i * 4
      statement.setString(row + 1, k)
      statement.setLong(row + 2, r.version)

      r.value match {
        case Flag(x) =>
          statement.setString(row + 3, Type.Flag)
          statement.setBoolean(row + 4, x)
        case Real(x) =>
          statement.setString(row + 3, Type.Real)
          statement.setDouble(row + 4, x)
        case Text(x) =>
          statement.setString(row + 3, Type.Text)
          statement.setString(row + 4, x)
        case None =>
          statement.setString(row + 3, Type.None)
          statement.setString(row + 4, "")
      }
    }

    statement.executeUpdate()
    statement.close()
  }

}