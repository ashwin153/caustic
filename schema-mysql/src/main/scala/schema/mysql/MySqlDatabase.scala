package schema.mysql

import com.mchange.v2.c3p0.ComboPooledDataSource
import java.io.Closeable
import java.sql.Connection
import scala.concurrent._
import scala.collection.mutable
import schema.runtime.{Database, Key, Revision, Value}

/**
 * A MySQL-backed database.
 *
 * @param underlying Underlying data source.
 */
class MySqlDatabase private[mysql](
  underlying: ComboPooledDataSource
) extends Database with Closeable {

  private def sql[R](f: Connection => R)(
    implicit ec: ExecutionContext
  ): Future[R] =
    Future {
      blocking {
        var con: Connection = null
        try {
          con = this.underlying.getConnection()
          f(con)
        } finally {
          if (con != null) con.close()
        }
      }
    }

  override def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, (Revision, Value)]] =
    if (keys.isEmpty)
      Future(Map.empty)
    else
      sql { con =>
        // Determine the versions and values of all keys.
        val smt = con.createStatement()
        val res = smt.executeQuery(
          s"""
             | SELECT `key`, `revision`, `value`
             | FROM `schema`.`schema`
             | WHERE `key` IN (${ keys.map("\"" + _ + "\"").mkString(",") })
           """.stripMargin
        )

        // Convert the result set into a Map.
        val buf = mutable.Buffer.empty[(Key, (Revision, Value))]
        while (res.next()) buf += res.getString(1) -> (res.getLong(2), res.getString(3))
        buf.toMap
      }

  override def put(depends: Map[Key, Revision], changes: Map[Key, (Revision, Value)])(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    sql { con =>
      // Filter out dependencies on objects that do not yet exist, sort the dependencies by version
      // and concatenate them into a comma delimited string.
      con.setAutoCommit(false)
      val requires = depends.values.filter(_ > 0).toSeq.sorted.mkString(",")

      // Update versions and values if and only if the corresponding revisions remain the same.
      val select = con.prepareStatement(
        s"""
           | SELECT GROUP_CONCAT(`revision` separator ',')
           | INTO @revisions
           | FROM `schema`.`schema`
           | WHERE `key` IN (${ depends.keys.map("\"" + _ + "\"").mkString(",") })
           | ORDER BY `revision`
         """.stripMargin)

      val conflicts = con.prepareStatement(
        s"""
           | SELECT @revisions != "$requires"
           | INTO @conflicts
         """.stripMargin)

      val insert = con.prepareStatement(
        s"""
           | INSERT IGNORE INTO `schema`.`schema` (`key`, `revision`, `value`) VALUES (?, 0, NULL)
         """.stripMargin)

      val update = con.prepareStatement(
        s"""
           | UPDATE `schema`.`schema`
           | SET `revision` = IF (@conflicts, `revision`, ?), `value` = IF (@conflicts, `value`, ?)
           | WHERE `key` = ?
         """.stripMargin)

      // Build and commit the transaction.
      if (depends.isEmpty) {
        val default = con.createStatement()
        default.execute("SET @conflicts = false")
      } else {
        select.executeQuery()
        conflicts.executeQuery()
      }

      changes.foreach { case (k, (r, v)) =>
        insert.setString(1, k)
        update.setLong(1, r)
        update.setString(2, v)
        update.setString(3, k)
        insert.executeUpdate()
        update.executeUpdate()
      }

      con.commit()

      // Return whether or not the transaction was successfully applied.
      con.setAutoCommit(true)
      val check = con.createStatement()
      val res = check.executeQuery("SELECT @conflicts AS conflicts")
      res.next()
      if (res.getBoolean("conflicts")) throw new Exception("Transaction conflicts.")
    }

  override def close(): Unit = this.underlying.close()

}

object MySqlDatabase {

  /**
   * Constructs a default connection pool to the specified MySQL database.
   *
   * @param host Database server address.
   * @param port Database server port.
   * @param user Database user.
   * @param password Database password.
   * @return MySQL Database.
   */
  def apply(host: String, port: Int, user: String, password: String, test: Boolean = false): MySqlDatabase = {
    val cpds = new ComboPooledDataSource()
    cpds.setDriverClass("com.mysql.cj.jdbc.Driver")
    cpds.setJdbcUrl(s"jdbc:mysql://${ host }:${ port }?useSSL=false&serverTimezone=UTC")
    cpds.setUser(user)
    cpds.setPassword(password)

    // Create the database tables if they do not exist.
    val con = cpds.getConnection()
    val smt = con.createStatement()

    // Delete the database every time we run tests.
    if (test) smt.execute(
      s"""
         | DROP DATABASE IF EXISTS `schema`
       """.stripMargin)

    smt.execute(
      s"""
         | CREATE DATABASE IF NOT EXISTS `schema`
       """.stripMargin)

    smt.execute(
      s"""
         | CREATE TABLE IF NOT EXISTS `schema`.`schema`(
         |   `key` varchar (250) NOT NULL,
         |   `revision` BIGINT DEFAULT 0,
         |   `value` TEXT,
         |   PRIMARY KEY(`key`)
         |)
       """.stripMargin)

    // Construct a MySQL Database.
    con.close()
    new MySqlDatabase(cpds)
  }

}