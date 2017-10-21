package caustic.runtime.sql

import caustic.runtime.{Key, Revision}
import java.sql.Connection

/**
 *
 */
trait Dialect {

  /**
   *
   * @return
   */
  def driver: String

  /**
   * A create table query that creates the table if and only if it doesn't exist.
   */
  def create(con: Connection): Unit

  /**
   * A select query for the key, versions and values of the specified keys.
   *
   * @param con JDBC connection.
   * @param keys Keys to select.
   * @return Selects keys from the database.
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
