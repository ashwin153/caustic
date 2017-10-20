package caustic.runtime
package local

import com.github.benmanes.caffeine.{cache => caffeine}
import com.typesafe.config.Config
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

/**
 * An in-memory, Caffeine database. Thread-safe.
 *
 * @param underlying Underlying data store.
 */
case class LocalDatabase(
  underlying: caffeine.Cache[Key, Revision]
) extends Database {

  override def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, Revision]] =
    Future(this.underlying.getAllPresent(keys.asJava).asScala.toMap)

  override def cput(depends: Map[Key, Version], changes: Map[Key, Revision])(
    implicit ec: ExecutionContext
  ): Future[Unit] =
    Future {
      this.synchronized {
        // Determine if the dependencies conflict with the underlying database.
        val conflicts = this.underlying
          .getAllPresent(depends.keys.asJava).asScala
          .filter { case (k, r) => depends(k) < r.version }

        // Throw an exception on conflict or perform updates otherwise.
        if (conflicts.isEmpty)
          this.underlying.putAll(changes.asJava)
        else
          throw ConflictException(conflicts.keySet.toSet)
      }
    }

  override def close(): Unit =
    this.underlying.invalidateAll()

}

object LocalDatabase {

  // Configuration root.
  val root: String = "caustic.runtime.database.local"

  /**
   * Constructs an empty, in-memory database.
   *
   * @return Empty LocalDatabase.
   */
  def apply(): LocalDatabase =
    LocalDatabase(caffeine.Caffeine.newBuilder().build[Key, Revision]())

  /**
   *
   * @param config
   * @return
   */
  def apply(config: Config): LocalDatabase =
    LocalDatabase()

}