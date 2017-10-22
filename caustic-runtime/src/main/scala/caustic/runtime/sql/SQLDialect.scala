package caustic.runtime
package sql

import caustic.runtime.sql.dialects.{MySQLDialect, PostgreSQLDialect}
import java.sql.Connection

/**
 * A JDBC-compatible, SQL implementation.
 */
trait SQLDialect {

  /**
   * Returns the JDBC driver class.
   *
   * @return JDBC driver.
   */
  def driver: String

  /**
   * Creates the database table if and only if it doesn't already exist.
   *
   * @param con JDBC connection.
   */
  def create(con: Connection): Unit

  /**
   * Returns the revisions of the selected keys.
   *
   * @param con JDBC connection.
   * @param keys Keys to select.
   * @return Revisions of all specified keys.
   */
  def select(con: Connection, keys: Set[Key]): Map[Key, Revision]

  /**
   * Bulk upserts the provided changes into the database.
   *
   * @param con JDBC connection.
   * @param changes Changes to apply.
   */
  def upsert(con: Connection, changes: Map[Key, Revision]): Unit

}

object SQLDialect {

  /**
   * Constructs a SQLDialect that corresponds to the specified name.
   * @param name SQLDialect name.
   * @return SQLDialect implementation.
   */
  def forName(name: String): SQLDialect = name match {
    case "mysql" => MySQLDialect
    case "postgresql" => PostgreSQLDialect
  }

}