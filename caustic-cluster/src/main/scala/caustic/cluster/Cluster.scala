package caustic.cluster

import java.io.Closeable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Random

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
  def size: Int = this.members.size

  /**
   * Performs the request on all members of the [[Cluster]] in parallel and returns their responses.
   *
   * @param request Request to perform.
   * @return Collection of responses.
   */
  def broadcast[R](request: C => R): Seq[Future[R]] = {
    val client = this.members.toSeq.map(service.connect)
    client.map(c => Future(request(c)) andThen { case _ => this.service.disconnect(c) })
  }

  /**
   * Performs the request on all members of the [[Cluster]] in parallel and returns their responses
   * if and only if a majority of the requests were successful.
   *
   * @param request Request to perform.
   * @return Collection of responses.
   */
  def quorum[R](request: C => R): Future[Seq[R]] = {
    val majority = Random.shuffle(this.members.toSeq).map(this.service.connect)
    Future.sequence(majority.map(c => Future(request(c)) andThen { case _ => this.service.disconnect(c) }))
  }

  /**
   * Performs the request on a randomly chosen member of the [[Cluster]] and returns the response.
   *
   * @param request Request to perform.
   * @return Collection of responses.
   */
  def random[R](request: C => R): Future[R] = {
    val client = service.connect(Random.shuffle(this.members).head)
    Future(request(client)) andThen { case _ => this.service.disconnect(client) }
  }

}