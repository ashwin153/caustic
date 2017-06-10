package caustic.postgres

import caustic.runtime._
import caustic.runtime.local.SQLDatabase
import javax.sql.DataSource

/**
 * A PostgreSQL-backed database.
 *
 * @param underlying Underlying database.
 */
class PostgresDatabase private[postgres](
  underlying: DataSource
) extends SQLDatabase(underlying) {

  override def select(keys: Iterable[Key]): String =
    s""" SELECT key, revision, value
       | FROM schema
       | WHERE key IN (${ keys.map("'" + _ + "'").mkString(",") })
     """.stripMargin

  override def update(key: Key, revision: Long, value: Value): String =
    s""" INSERT INTO schema (key, revision, value)
       | VALUES ('$key', $revision, '$value') ON CONFLICT (key)
       | DO UPDATE SET revision = $revision, value = '$value'
     """.stripMargin

}

object PostgresDatabase {


  /**
   * Constructs a Postgres database backed by the specified data source.
   *
   * @param source Data source.
   * @return Postgres database.
   */
  def apply(source: DataSource): PostgresDatabase = {
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

    // Construct a Postgres Database.
    con.close()
    new PostgresDatabase(source)
  }

}
