package schema.mysql

import javax.sql.DataSource

/**
 * A MySQL-backed database.
 *
 * @param underlying Underlying database.
 */
class RocksDBDatabase(
  rocks: RocksDB
) extends Database {

}

object RocksDBDatabase {

  /**
   *
   * @param source
   * @return
   */
  def apply(source: DataSource): RocksDBDatabase = {
    // Construct the database tables if they do not already exist.
    val con = source.getConnection()
    val smt = con.createStatement()

    smt.execute(
      s""" CREATE TABLE IF NOT EXISTS `schema`(
         |   `key` varchar (1000) NOT NULL,
         |   `revision` BIGINT DEFAULT 0,
         |   `value` TEXT,
         |   PRIMARY KEY(`key`)
         | )
       """.stripMargin)

    // Construct a MySQL Database.
    con.close()
    new RocksDBDatabase(source)
  }

}