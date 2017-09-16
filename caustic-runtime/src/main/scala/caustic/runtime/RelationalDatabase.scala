package caustic.runtime

import java.sql.Connection
import javax.sql.DataSource
import scala.concurrent.{ExecutionContext, Future, blocking}

/**
 * A transactional, SQL database.
 *
 * @param underlying Underlying store.
 */
abstract class RelationalDatabase(underlying: DataSource)(
  implicit ec: ExecutionContext
) extends Database {

  // Ensure that the table schema exists.
  this.transaction { con =>
    val smt = con.createStatement()
    smt.execute(this.schema)
    smt.close()
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
        con.setAutoCommit(false)
        val res = f(con)
        con.commit()
        con.setAutoCommit(true)
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
      // Determine whether or not the transaction conflicts.
      val current = if (depends.isEmpty) Map.empty[Key, Revision] else select(con, depends.keySet)
      val conflicts = depends.exists { case (k, v) => current.get(k).exists(_.version > v) }

      if (!conflicts) {
        // If the transaction does not conflict, then perform changes.
        changes.foreach { case (k, r) => upsert(con, k, r) }
      } else {
        // Otherwise, fail the put operation.
        throw new Exception("Transaction conflicts.")
      }
    }

}
