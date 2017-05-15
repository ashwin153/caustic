package schema.runtime.local

import SQLDatabase._
import java.sql.Connection
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
  ): Future[Map[Key, (Revision, Value)]] = {
    if (keys.isEmpty) Future(Map.empty) else sql(this.underlying) { con =>
      // Determine the versions and values of all keys.
      val smt = con.createStatement()
      val res = smt.executeQuery(select(keys))
      con.commit()

      // Convert the result set into a Map.
      val buf = mutable.Buffer.empty[(Key, (Revision, Value))]
      while (res.next()) buf += res.getString(1) -> (res.getLong(2), res.getString(3))
      buf.toMap
    }
  }

  override def put(depends: Map[Key, Revision], changes: Map[Key, (Revision, Value)])(
    implicit ec: ExecutionContext
  ): Future[Unit] = sql(this.underlying) { con =>
    // Determine whether or not the transaction conflicts.
    var error = false
    if (depends.nonEmpty) {
      val verify = con.prepareStatement(conflicts(depends))
      val res = verify.executeQuery()
      res.next()
      error = res.getBoolean(1)
      res.close()
      verify.close()
    }

    if (error) {
      // Rollback the transaction on failure and report an error.
      con.rollback()
      throw new Exception("Transaction conflicts.")
    } else {
      // Otherwise, perform changes.
      changes.foreach { case (k, (r, v)) =>
        val upsert = con.prepareStatement(update(k, r, v))
        upsert.executeUpdate()
        upsert.close()
      }

      con.commit()
    }
  }

  /**
   *
   * @param keys
   * @return
   */
  def select(keys: Set[Key]): String

  /**
   *
   * @param depends
   * @return
   */
  def conflicts(depends: Map[Key, Revision]): String

  /**
   *
   * @param key
   * @param revision
   * @param value
   * @return
   */
  def update(key: Key, revision: Long, value: Value): String

}

object SQLDatabase {

  /**
   *
   * @param source
   * @param f
   * @param ec
   * @tparam R
   * @return
   */
  def sql[R](source: DataSource)(f: Connection => R)(
    implicit ec: ExecutionContext
  ): Future[R] =
    Future {
      blocking {
        var con: Connection = null
        try {
          con = source.getConnection()
          con.setAutoCommit(false)
          f(con)
        } finally {
          if (con != null) {
            con.setAutoCommit(true)
            con.close()
          }
        }
      }
    }

}