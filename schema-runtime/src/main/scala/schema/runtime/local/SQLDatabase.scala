package schema.runtime.local

import SQLDatabase._
import java.sql.{Connection, SQLException}
import javax.sql.DataSource
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, blocking}
import schema.runtime.{Database, Key, Revision, Value}

/**
 * An abstract SQL database.
 *
 * @param underlying Underlying store.
 */
abstract class SQLDatabase(underlying: DataSource) extends Database {

  override def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, (Revision, Value)]] =
    if (keys.isEmpty)
      Future(Map.empty)
    else sql(this.underlying) { con =>
      // Determine the versions and values of all keys.
      val smt = con.createStatement()
      val res = smt.executeQuery(select(keys))

      // Convert the result set into a Map.
      val buf = mutable.Buffer.empty[(Key, (Revision, Value))]
      while (res.next()) buf += res.getString(1) -> (res.getLong(2), res.getString(3))

      // Cleanup result set and statement and return.
      res.close()
      smt.close()
      buf.toMap
    }

  override def put(depends: Map[Key, Revision], changes: Map[Key, (Revision, Value)])(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    sql(this.underlying) { con =>
      var error = false
      if (depends.nonEmpty) {
        // Determine whether or not the transaction conflicts.
        val verify = con.prepareStatement(conflicts(depends))
        val res = verify.executeQuery()
        res.next()
        error = res.getBoolean(1)

        // Cleanup result set and statement.
        res.close()
        verify.close()
      }

      if (error) {
        // Throw an exception on error.
        throw new SQLException("Transaction conflicts.")
      } else {
        // Otherwise, perform changes.
        changes.foreach { case (k, (r, v)) =>
          val upsert = con.prepareStatement(update(k, r, v))
          upsert.executeUpdate()
          upsert.close()
        }
      }
    }

  /**
   * A select query for the key, versions and values of the specified keys.
   *
   * @param keys Keys to select.
   * @return SQL select query.
   */
  def select(keys: Set[Key]): String

  /**
   * A SQL query that determines whether or not the specified dependencies conflict.
   *
   * @param depends Dependencies to check.
   * @return SQL conflict detection query.
   */
  def conflicts(depends: Map[Key, Revision]): String

  /**
   * A upsert query that inserts or updates the specified key, revision, and value.
   *
   * @param key Key to upsert.
   * @param revision Revision of key.
   * @param value Value of key.
   * @return SQL update query.
   */
  def update(key: Key, revision: Long, value: Value): String

}

object SQLDatabase {

  /**
   * Performs the specified transaction on the underying database.
   *
   * @param source Database.
   * @param txn Transaction.
   * @param ec Implicit execution context.
   * @tparam R Type of return value.
   * @return Result of performing the transaction.
   */
  private def sql[R](source: DataSource)(txn: Connection => R)(
    implicit ec: ExecutionContext
  ): Future[R] = {
    var con: Connection = null

    Future(
      blocking {
        con = source.getConnection()
        con.setAutoCommit(false)
        val res = txn(con)
        con.commit()
        con.setAutoCommit(true)
        con.close()
        res
      }
    ).recoverWith {
      case e: Exception if con != null =>
        con.rollback()
        con.close()
        con.setAutoCommit(true)
        Future.failed(e)
    }
  }

}