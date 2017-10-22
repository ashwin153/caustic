package caustic.runtime
package sql

import caustic.runtime.sql.SQLDatabase._
import caustic.runtime.sql.dialects.{MySQLDialect, PostgreSQLDialect}

import com.mchange.v2.c3p0.{ComboPooledDataSource, PooledDataSource}
import pureconfig._

import java.sql.Connection
import javax.sql.DataSource
import scala.concurrent.{ExecutionContext, Future, blocking}

/**
 * A transactional, SQL database. Thread-safe.
 *
 * @param underlying Underlying C3P0 connection pool.
 * @param dialect SQL implementation.
 */
case class SQLDatabase(
  underlying: PooledDataSource,
  dialect: SQLDialect
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
      if (conflicts.nonEmpty) {
        throw ConflictException(conflicts.keys.toSet)
      } else if (changes.nonEmpty) {
        this.dialect.upsert(con, changes)
      }
    }

  override def close(): Unit =
    this.underlying.close()

}

object SQLDatabase {

  // Implicit global execution context.
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  /**
   * A SQLDatabase configuration.
   *
   * @param username Database username.
   * @param password Database password.
   * @param dialect SQLDialect name. ("mysql", "postgresql")
   * @param url JDBC connection url.
   */
  case class Config(
    username: String,
    password: String,
    dialect: String,
    url: String
  )

  /**
   * Constructs a SQLDatabase by loading the configuration from the classpath.
   *
   * @return Classpath-configured SQLDatabase.
   */
  def apply(): SQLDatabase =
    SQLDatabase(loadConfigOrThrow[Config]("caustic.database.sql"))

  /**
   * Constructs a SQLDatabase from the provided configuration.
   *
   * @param config Configuration.
   * @return Dynamically-configured SQLDatabase.
   */
  def apply(config: Config): SQLDatabase = {
    // Determine the underlying SQL dialect.
    val dialect = SQLDialect.forName(config.dialect)

    // Setup a C3P0 connection pool.
    val pool = new ComboPooledDataSource()
    pool.setUser(config.username)
    pool.setPassword(config.password)
    pool.setDriverClass(dialect.driver)
    pool.setJdbcUrl(config.url)

    // Construct the corresponding database.
    SQLDatabase(pool, dialect)
  }

  /**
   * Asynchronously executes the specified transaction on the provided DataSource and returns the
   * result. Transactions are executed with a SERIALIZABLE isolation level, and are automatically
   * rolled back on failure.
   *
   * @param source DataSource.
   * @param f Database transaction.
   * @param ec Implicit execution context.
   * @return Result of transaction execution.
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
        con.close()
        res
      }
    } recoverWith {
        case e: Exception if con != null =>
          con.rollback()
          con.close()
          Future.failed(e)
    }
  }

}