package caustic.beaker

import caustic.beaker.thrift.{Revision, Transaction}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
 * A write-through cache.
 */
trait Cache extends Database {

  /**
   * Returns the underlying [[Database]].
   *
   * @return Underlying [[Database]].
   */
  def database: Database

  /**
   * Returns the cached [[Revision]] of the specified keys.
   *
   * @param keys Keys to fetch.
   * @return Cached [[Revision]] of the keys.
   */
  def fetch(keys: Set[Key]): Try[Map[Key, Revision]]

  /**
   * Applies the changes to the [[Cache]].
   *
   * @param changes Changes to apply.
   */
  def update(changes: Map[Key, Revision]): Try[Unit]

  override def read(keys: Set[Key]): Try[Map[Key, Revision]] = {
    fetch(keys) recover { case _ => Map.empty[Key, Revision] } flatMap { hits =>
      val misses = hits.keySet diff keys
      if (misses.nonEmpty) {
        // Reload any cache misses from the underlying database.
        this.database.read(misses) map  { changes =>
          update(changes)
          hits ++ changes
        }
      } else {
        // Return the cache hits otherwise.
        Success(hits)
      }
    }
  }

  override def write(changes: Map[Key, Revision]): Try[Unit] = {
    // Write to the database and then update the cache.
    this.database.write(changes).flatMap(_ => this.update(changes))
  }

  override def commit(transaction: Transaction): Try[Unit] = {
    this.database.commit(transaction) match {
      case Success(_) =>
        // Update the values of changed keys.
        update(transaction.changes.asScala.toMap)
      case Failure(Database.Conflicts(invalid)) =>
        // Update the invalid keys and propagate failures.
        update(invalid)
        Failure(Database.Conflicts(invalid))
      case Failure(e) =>
        // Propagate all other failures.
        Failure(e)
    }
  }

  override def close(): Unit = {
    // Propagate close to the underlying database.
    this.database.close()
  }

}