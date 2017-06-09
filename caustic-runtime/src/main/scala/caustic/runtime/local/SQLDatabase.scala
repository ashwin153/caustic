package caustic.runtime
package local

import SQLDatabase._
import java.sql.{Connection, ResultSet, SQLException, Statement}
import javax.sql.DataSource
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, blocking}

/**
 * An abstract SQL database.
 *
 * @param underlying Underlying store.
 */
abstract class SQLDatabase(underlying: DataSource) extends Database {

  /**
   * A select query for the key, versions and values of the specified keys.
   *
   * @param keys Keys to select.
   * @return SQL select query.
   */
  def select(keys: Iterable[Key]): String

  /**
   * A upsert query that inserts or updates the specified key, revision, and value.
   *
   * @param key Key to upsert.
   * @param revision Revision of key.
   * @param value Value of key.
   * @return SQL update query.
   */
  def update(key: Key, revision: Long, value: Value): String

  override def get(keys: Iterable[Key])(
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
      while (res.next()) buf += res.getString(1) -> ((res.getLong(2), res.getString(3)))

      // Cleanup result set and statement and return.
      res.close()
      smt.close()
      buf.toMap
    }

  override def put(depends: Map[Key, Revision], changes: Map[Key, (Revision, Value)])(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    sql(this.underlying) { con =>
      // Determine whether or not the transaction conflicts.
      def conflicts: Boolean = {
        val smt: Statement = con.createStatement()
        val res: ResultSet = smt.executeQuery(select(depends.keySet))

        while (res.next())
          if (depends(res.getString(1)) != res.getLong(2))
            return true
        false
      }

      // Conditionally perform modifications if the transaction does not conflict.
      if (depends.isEmpty || !conflicts) {
        changes.foreach { case (k, (r, v)) =>
          val upsert = con.prepareStatement(update(k, r, v))
          upsert.executeUpdate()
          upsert.close()
        }
      } else {
        throw new SQLException("Transaction conflicts.")
      }
    }

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

    Future {
      blocking {
        con = source.getConnection()
        con.setAutoCommit(false)
        val res = txn(con)
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

}
