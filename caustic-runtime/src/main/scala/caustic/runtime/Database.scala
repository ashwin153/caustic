package caustic.runtime

import beaker.client._
import beaker.common.concurrent._
import beaker.server.protobuf._

import scala.collection.mutable
import scala.util.Try

/**
 * A transactional key-value store.
 */
trait Database {

  /**
   * Returns the revisions of the specified keys.
   *
   * @param keys Keys to retrieve.
   * @return Revisions of keys.
   */
  def get(keys: Set[Key]): Try[Map[Key, Revision]]

  /**
   * Conditionally applies the changes if the dependencies remain unchanged.
   *
   * @param depends Dependencies.
   * @param changes Conditional updates.
   * @return Whether or not the changes were applied.
   */
  def cas(depends: Map[Key, Version], changes: Map[Key, Value]): Try[Unit]

}

object Database {

  /**
   * An in-memory, thread-safe database. Useful for testing.
   *
   * @param underlying Underlying map.
   */
  class Local(underlying: mutable.Map[Key, Revision]) extends Database with Locking {

    override def get(keys: Set[Key]): Try[Map[Key, Revision]] = shared {
      Try(this.underlying.filterKeys(keys.contains).toMap)
    }

    override def cas(depends: Map[Key, Version], changes: Map[Key, Value]): Try[Unit] = exclusive {
      Try(this.underlying.filterKeys(depends.contains))
        .filter(_ forall { case (k, v) => depends(k) >= v.version })
        .map(_ => this.underlying ++= changes map { case (k, v) => k -> Revision(depends(k)+1, v) })
    }

  }

  object Local {

    /**
     * Constructs an in-memory database initialized with the specified values.
     *
     * @param initial Initial values.
     * @return Initialized local database.
     */
    def apply(initial: (Key, Revision)*): Database.Local =
      Database.Local(initial.toMap)

    /**
     * Constructs an in-memory database initialized with the specified values.
     *
     * @param initial Initialized values.
     * @return Initialized local database.
     */
    def apply(initial: Map[Key, Revision]): Database.Local =
      new Database.Local(mutable.Map(initial.toSeq: _*))

  }

  /**
   * A Beaker database.
   *
   * @param client Beaker client.
   */
  class Beaker(client: Client) extends Database {

    override def get(keys: Set[Key]): Try[Map[Key, Revision]] =
      this.client.get(keys)

    override def cas(depends: Map[Key, Version], changes: Map[Key, Value]): Try[Unit] =
      this.client.cas(depends, changes).map(_ => ())

  }

  object Beaker {

    /**
     * Constructs a Beaker database connected to the specified address.
     *
     * @param name Hostname.
     * @param port Port number.
     * @return Connected beaker database.
     */
    def apply(name: String, port: Int): Database.Beaker =
      Database.Beaker(Address(name, port))

    /**
     * Constructs a Beaker database connected to the specified address.
     *
     * @param address Network location.
     * @return Connected beaker database.
     */
    def apply(address: Address): Database.Beaker =
      Database.Beaker(Client(address))

    /**
     * Constructs a Beaker database connected to the specified client.
     *
     * @param client Beaker client.
     * @return Connected beaker database.
     */
    def apply(client: Client): Database.Beaker =
      new Database.Beaker(client)

  }

}
