package caustic.runtime

import scala.concurrent.{ExecutionContext, Future}

/**
 * An asynchronous, non-transactional key-value store.
 */
trait Cache {

  /**
   * Asynchronously returns the cached revisions of the specified keys.
   *
   * @param keys Keys to fetch.
   * @param ec Implicit execution context.
   * @return Cached revisions of the specified keys.
   */
  def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, Revision]]

  /**
   * Asynchronously applies the specified changes to the cache.
   *
   * @param changes Updates to make.
   * @param ec Implicit execution context.
   * @return Future that completes when successful, or an exception otherwise.
   */
  def put(changes: Map[Key, Revision])(
    implicit ec: ExecutionContext
  ): Future[Unit]

  /**
   * Asynchronously removes the specified keys from the cache.
   *
   * @param keys Keys to purge.
   * @param ec Implicit execution context.
   * @return Future that completes when successful, or an exception otherwise.
   */
  def invalidate(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Unit]

}

