package caustic.runtime
package jdbc

import java.sql.Connection
import scala.collection.mutable

/**
 *
 */
object PostgresDialect extends Dialect {

  override val driver: String = "org.postgresql.Driver"

  override def create(connection: Connection): Unit =
    connection.createStatement().execute(
      s""" CREATE TABLE IF NOT EXISTS caustic (
         |   key varchar (1000) NOT NULL,
         |   version BIGINT DEFAULT 0,
         |   type INT,
         |   value TEXT,
         |   PRIMARY KEY(key)
         | )
     """.stripMargin)

  override def select(connection: Connection, keys: Set[Key]): Map[Key, Revision] = {
    // Load all the rows that match the keys.
    val statement = connection.prepareStatement(
      s""" SELECT key, version, type, value
         | FROM caustic
         | WHERE key IN (${List.fill(keys.size)("?").mkString(",")})
       """.stripMargin
    )

    keys.zipWithIndex.foreach { case (k, i) => statement.setString(i + 1, k) }
    val results = statement.executeQuery()

    // Parse the result set into a local buffer.
    val buffer = mutable.Buffer.empty[(Key, Revision)]
    while (results.next()) {
      val key = results.getString("key")
      val version = results.getLong("version")
      val value = results.getInt("type") match {
        case 0 => flag(results.getBoolean("value"))
        case 1 => real(results.getDouble("value"))
        case 2 => text(results.getString("value"))
        case 3 => None
      }

      buffer += (key -> Revision(version, value))
    }

    // Close the statement and return buffer.
    results.close()
    statement.close()
    buffer.toMap
  }

  override def upsert(connection: Connection, changes: Map[Key, Revision]): Unit = {
    // Prepare the SQL statement.
    val statement = connection.prepareStatement(
      s""" INSERT INTO caustic (key, version, type, value)
         | VALUES ${Seq.fill(changes.size)("(?, ?, ?, ?)").mkString(",")}
         | ON CONFLICT (key) DO UPDATE SET
         | version = excluded.version,
         | type = excluded.type,
         | value = excluded.value
     """.stripMargin)

    changes.zipWithIndex foreach { case ((k, r), i) =>
      statement.setString(i*4 + 1, k)
      statement.setLong(i*4 + 2, r.version)

      r.value match {
        case Flag(x) =>
          statement.setInt(i*4 + 3, 0)
          statement.setBoolean(i*4 + 4, x)
        case Real(x) =>
          statement.setInt(i*4 + 3, 1)
          statement.setDouble(i*4 + 4, x)
        case Text(x) =>
          statement.setInt(i*4 + 3, 2)
          statement.setString(i*4 + 4, x)
        case None =>
          statement.setInt(i*4 + 3, 3)
          statement.setString(i*4 + 4, "")
      }
    }

    // Execute and close the statement.
    statement.executeUpdate()
    statement.close()
  }

}
