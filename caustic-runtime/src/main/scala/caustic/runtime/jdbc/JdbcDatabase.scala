package caustic.runtime
package jdbc

import JdbcDatabase._

import com.mchange.v2.c3p0.{ComboPooledDataSource, PooledDataSource}
import pureconfig._

import java.sql.Connection
import javax.sql.DataSource
import scala.concurrent.{ExecutionContext, Future, blocking}

/**
 * A transactional, SQL database.
 *
 * @param underlying Underlying store.
 */
case class JdbcDatabase(
  underlying: PooledDataSource,
  dialect: Dialect
) extends Database {

  // Verify that the table schema exists.
  val exists: Future[Unit] = transaction(this.underlying)(this.dialect.create)

  override def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, Revision]] =
    if (keys.isEmpty)
      Future(Map.empty)
    else
      transaction(this.underlying)(this.dialect.select(_, keys))

  override def cput(depends: Map[Key, Version], changes: Map[Key, Revision])(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    transaction(this.underlying) { con =>
      // Determine if the dependencies conflict with the underlying database.
      val current = if (depends.isEmpty) Map.empty else this.dialect.select(con, depends.keySet)
      val conflicts = current filter { case (k, r) => depends(k) < r.version }

      // Throw an exception on conflict or perform updates otherwise.
      if (conflicts.isEmpty) {
        this.dialect.upsert(con, changes)
      } else {
        throw ConflictException(conflicts.keys.toSet)
      }
    }

  override def close(): Unit =
    this.underlying.close()

}

object JdbcDatabase {

  // Implicit global execution context.
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  /**
   *
   * @param username
   * @param password
   * @param dialect
   * @param url
   */
  case class Config(
    username: String,
    password: String,
    dialect: String,
    url: String
  )

  /**
   *
   * @return
   */
  def apply(): JdbcDatabase =
    JdbcDatabase(loadConfigOrThrow[Config]("caustic.database.jdbc"))

  /**
   *
   * @param config
   * @return
   */
  def apply(config: Config): JdbcDatabase =
    JdbcDatabase(config.username, config.password, config.dialect, config.url)

  /**
   *
   * @param username
   * @param password
   * @param dialect
   * @param url
   */
  def apply(
    username: String,
    password: String,
    dialect: String,
    url: String
  ): JdbcDatabase = {
    // Determine the underlying SQL dialect.
    val underlying = dialect match {
      case "mysql" => MySQLDialect
      case "postgres" => PostgresDialect
    }

    // Setup a C3P0 connection pool.
    val pool = new ComboPooledDataSource()
    pool.setUser(username)
    pool.setPassword(password)
    pool.setDriverClass(underlying.driver)
    pool.setJdbcUrl(url)

    // Construct the corresponding database.
    JdbcDatabase(pool, underlying)
  }

  /**
   *
   * @param source
   * @param f
   * @param ec
   * @tparam R
   * @return
   */
  def transaction[R](source: DataSource)(f: Connection => R)(
    implicit ec: ExecutionContext
  ): Future[R] = {
    var con: Connection = null
    Future {
      blocking {
        con = source.getConnection()
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
          con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED)
          con.close()
          Future.failed(e)
    }
  }

}