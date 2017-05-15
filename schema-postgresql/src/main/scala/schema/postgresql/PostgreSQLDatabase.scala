package schema.postgresql

import javax.sql.DataSource
import schema.runtime.local.SQLDatabase
import schema.runtime.{Key, Revision, Value}

/**
 * A PostgreSQL-backed database.
 *
 * @param underlying
 */
class PostgreSQLDatabase private[postgresql](
  underlying: DataSource
) extends SQLDatabase(underlying) {

  override def select(keys: Set[Key]): String =
    s""" SELECT key, revision, value
       | FROM schema
       | WHERE key IN (${ keys.map("'" + _ + "'").mkString(",") })
     """.stripMargin

  override def conflicts(depends: Map[Key, Revision]): String =
    s""" SELECT '${ depends.values.filter(_ > 0).toSeq.sorted.mkString(",") }' !=
       | string_agg(cast(revision AS text), ',' ORDER BY revision)
       | FROM schema
       | WHERE key IN (${ depends.keys.map("'" + _ + "'").mkString(",") })
     """.stripMargin

  override def update(key: Key, revision: Long, value: Value): String =
    s""" INSERT INTO schema (key, revision, value)
       | VALUES ('$key', $revision, '$value') ON CONFLICT (key)
       | DO UPDATE SET revision = $revision, value = '$value'
     """.stripMargin

}

object PostgreSQLDatabase {


  /**
   *
   * @param source
   * @return
   */
  def apply(source: DataSource): PostgreSQLDatabase = {
    // Construct the database tables if they do not already exist.
    val con = source.getConnection()
    val smt = con.createStatement()

    smt.execute(
      s""" CREATE TABLE IF NOT EXISTS schema(
         |   key varchar (1000) NOT NULL,
         |   revision BIGINT DEFAULT 0,
         |   value TEXT,
         |   PRIMARY KEY(key)
         | )
       """.stripMargin)

    // Construct a MySQL Database.
    con.close()
    new PostgreSQLDatabase(source)
  }

}