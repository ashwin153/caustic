package caustic.runtime

import scala.concurrent.Future

/**
 * A cached key-value store.
 */
trait Cache extends Database {

  /**
   * Asynchronously retrieves the specified keys from the database.
   *
   * @param keys Keys to retrieve.
   * @return Cached revisions of the specified keys.
   */
  def fetch(keys: Set[Key]): Future[Map[Key, Revision]]

  /**
   * Asynchronously applies the specified changes to the database.
   *
   * @param changes Changes to apply.
   */
  def update(changes: Map[Key, Revision]): Future[Unit]

  /**
   * Asynchronously removes the specified keys from the cache.
   *
   * @param keys Keys to remove.
   */
  def invalidate(keys: Set[Key]): Future[Unit]

}
