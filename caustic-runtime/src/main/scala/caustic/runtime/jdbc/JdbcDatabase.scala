package caustic.runtime
package jdbc

import com.mchange.v2.c3p0.{ComboPooledDataSource, PooledDataSource}
import com.typesafe.config.Config
import java.sql.Connection
import scala.concurrent.{ExecutionContext, Future, blocking}

/**
 * A transactional, SQL database.
 *
 * @param underlying Underlying store.
 */
abstract class JdbcDatabase(
  underlying: PooledDataSource
) extends Database {

  // Ensure that the table schema exists.
  val exists: Future[Unit] = this.transaction { con =>
    val smt = con.createStatement()
    smt.execute(this.schema)
    smt.close()
  } {
    scala.concurrent.ExecutionContext.global
  }

  /**
   * Returns the SQL query that creates the table schema if and only if it doesn't already exist.
   *
   * @return SQL create table if not exists query.
   */
  def schema: String

  /**
   * A select query for the key, versions and values of the specified keys.
   *
   * @param con JDBC connection.
   * @param keys Keys to select.
   * @return SQL select query.
   */
  def select(con: Connection, keys: Set[Key]): Map[Key, Revision]

  /**
   * A upsert query that inserts or updates the specified key, revision, and value.
   *
   * @param con JDBC connection.
   * @param key Key to upsert.
   * @param revision Revision to upsert.
   * @return SQL update query.
   */
  def upsert(con: Connection, key: Key, revision: Revision): Unit

  /**
   * Asynchronously performs the JDBC transaction on the underlying DataSource. Transactions are
   * modeled as method calls on a JDBC connection that atomically commit and safely rollback.
   *
   * @param f Transaction function.
   * @param ec Implicit execution context.
   * @tparam R Type of return value.
   * @return Result of performing the transaction.
   */
  def transaction[R](f: Connection => R)(
    implicit ec: ExecutionContext
  ): Future[R] = {
    var con: Connection = null
    Future {
      blocking {
        con = this.underlying.getConnection()
        con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE)
        con.setAutoCommit(false)
        val res = f(con)
        con.commit()
        con.setAutoCommit(true)
        con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED)
        con.close()
        res
      }
    } recoverWith {
      case e: Exception if con != null =>
        con.rollback()
        con.setAutoCommit(true)
        con.close()
        Future.failed(e)
    }
  }

  override def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, Revision]] =
    if (keys.isEmpty)
      Future(Map.empty)
    else
      transaction(select(_, keys))

  override def cput(depends: Map[Key, Version], changes: Map[Key, Revision])(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    transaction { con =>
      // Determine if the dependencies conflict with the underlying database.
      val current = if (depends.isEmpty) Map.empty[Key, Revision] else select(con, depends.keySet)
      val conflicts = current filter { case (k, r) => depends(k) < r.version }

      // Throw an exception on conflict or perform updates otherwise.
      if (conflicts.isEmpty) {
        changes.foreach { case (k, r) => upsert(con, k, r) }
      } else {
        throw ConflictException(conflicts.keySet)
      }
    }

  override def close(): Unit =
    this.underlying.close()

}

object JdbcDatabase {

  // Configuration root.
  val root: String = "caustic.runtime.database.jdbc"

  // Driver classes for the various supported dialects.
  val drivers: Map[String, String] = Map(
    "mysql" -> "com.mysql.cj.jdbc.Driver",
    "postgres" -> "org.postgresql.Driver"
  )

  /**
   *
   * @param config
   * @return
   */
  def apply(config: Config): JdbcDatabase = {
    // Setup a C3P0 connection pool.
    val pool = new ComboPooledDataSource()
    pool.setUser(config.getString(s"$root.username"))
    pool.setPassword(config.getString(s"$root.password"))
    pool.setDriverClass(drivers(config.getString(s"$root.dialect")))
    pool.setJdbcUrl(config.getString(s"$root.url"))

    // Construct the corresponding database.
    config.getString(s"$root.dialect") match {
      case "mysql" => MySQLDatabase(pool)
      case "postgres" => PostgresDatabase(pool)
    }
  }

}