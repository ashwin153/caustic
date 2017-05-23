package schema.mysql

import javax.sql.DataSource
import schema.runtime.local.SQLDatabase
import schema.runtime.{Key, Revision, Value}

/**
 * A MySQL-backed database.
 *
 * @param underlying Underlying database.
 */
class MySQLDatabase private[mysql](
  underlying: DataSource
) extends SQLDatabase(underlying) {

  override def select(keys: Set[Key]): String =
    s""" SELECT `key`, `revision`, `value`
       | FROM `schema`
       | WHERE `key` IN (${ keys.map("\"" + _ + "\"").mkString(",") })
     """.stripMargin

  override def update(key: Key, revision: Revision, value: Value): String =
    s""" INSERT INTO `schema` (`key`, `revision`, `value`)
       | VALUES ("$key", $revision, "$value")
       | ON DUPLICATE KEY UPDATE revision = $revision, value = "$value"
     """.stripMargin

}

object MySQLDatabase {

  /**
   *
   * @param source
   * @return
   */
  def apply(source: DataSource): MySQLDatabase = {
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
    new MySQLDatabase(source)
  }

}