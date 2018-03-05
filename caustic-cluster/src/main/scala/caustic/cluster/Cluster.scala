package caustic.service

import java.io.Closeable

import scala.collection.mutable
import scala.util.{Random, Try}

/**
 *
 */
trait Cluster[C] extends Closeable {

  /**
   *
   * @return
   */
  def service: Service[C]

  /**
   * Returns the known members of the [[Cluster]].
   *
   * @return Current members.
   */
  def members: Set[Address]

  /**
   * Adds the instance to the [[Cluster]].
   *
   * @param instance Instance [[Address]].
   */
  def join(instance: Address): Unit

  /**
   * Removes the instance from the [[Cluster]].
   *
   * @param instance Instance [[Address]].
   */
  def leave(instance: Address): Unit

  /**
   * Returns the number of members.
   *
   * @return Number of members.
   */
  final def size: Int = this.members.size

  /**
   * Performs the request on all members of the [[Cluster]] in parallel and returns their responses.
   *
   * @param request Request to perform.
   * @return Collection of responses.
   */
  final def broadcast[R](request: C => R): Seq[Try[R]] = {
    val client = this.members.map(service.connect).toSeq
    val result = client.par.map(c => Try(request(c))).seq
    client.foreach(this.service.disconnect)
    result
  }

  /**
   * Performs the request on all members of the [[Cluster]] in parallel and returns their responses
   * if and only if a majority of the requests were successful.
   *
   * @param request Request to perform.
   * @return Collection of responses.
   */
  final def quorum[R](request: C => R): Try[Seq[R]] = {
    Try(broadcast(request).filter(_.isSuccess).map(_.get)).filter(_.size >= this.size / 2 + 1)
  }

  /**
   * Performs the request on a randomly chosen member of the [[Cluster]] and returns the response.
   *
   * @param request Request to perform.
   * @tparam R
   * @return Collection of responses.
   */
  final def random[R](request: C => R): Try[R] = {
    val client = service.connect(Random.shuffle(this.members).head)
    val result = Try(request(client))
    service.disconnect(client)
    result
  }

}