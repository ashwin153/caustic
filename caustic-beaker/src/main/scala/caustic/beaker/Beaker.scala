package caustic.beaker

import caustic.beaker.common._
import caustic.beaker.storage.Local
import caustic.beaker.thrift._
import caustic.cluster.protocol.Thrift
import caustic.cluster.{Address, Announcer, Cluster}

import org.apache.thrift.async.AsyncMethodCallback

import java.io.Closeable
import java.{lang, util}
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.math.Ordering.Implicits._
import scala.util.{Failure, Success}

/**
 * A distributed, transactional key-value store.
 *
 * @param id Unique identifier.
 * @param database Underlying database.
 * @param scheduler Transaction scheduler.
 * @param cluster Beaker cluster.
 * @param backoff Consensus backoff duration.
 */
case class Beaker(
  id: Int,
  database: Database,
  scheduler: Scheduler[Transaction],
  cluster: Cluster[Internal.Client],
  backoff: Duration = 1 second
) extends thrift.Beaker.AsyncIface with Closeable {

  val round    : AtomicInteger          = new AtomicInteger(1)
  val promised : mutable.Set[Proposal]  = mutable.Set.empty
  val accepted : mutable.Set[Proposal]  = mutable.Set.empty
  val learned  : History[Ballot]        = History(100)

  // IF WE LEARN A TRANSACTION THAT PROPOSAL APPLIES THAT EQUALS A TRANSACTION WE PROPOSED WE FINISH.
  // IF WE LEARN A TRANSACTION THAT PROPOSAL REPAIRS THAT CONFLICTS WITH A TRANSACTION WE PROPOSED WE CANCEL.
  // IGNORE REPAIRS.
  // USE THE SAME QUORUMS FOR PREPARE/READ/ACCEPT/LEARN
  // WHEN A PROPOSAL IS LEARNED, SHARE THE TRANSACTIONS WE LEARNED THAT WERE APPLIED.
  // WE HAVE TO DEPEND ON EVERY KEY WE WRITE FOR EVERYTHING BUT REPAIRS.
  // IF WE LEARN A PROPOSAL THAT WRITES A KEY WE READ WE TERMINATE CONSENSUS.

  /**
   * Coordinate consensus on a proposal. Uses a variation of Generalized Paxos that has several
   * desirable properties. First, beakers may simultaneously commit non-conflicting transactions.
   * Second, beakers automatically repair replicas that have stale revisions. Third, beakers may
   * safely commit transactions as long as they are connected to at least a majority of their
   * non-faulty peers.
   *
   * @see https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/tr-2005-33.pdf
   * @see https://www.datastax.com/dev/blog/lightweight-transactions-in-cassandra-2-0
   * @see https://www.cs.cmu.edu/~dga/papers/epaxos-sosp2013.pdf
   *
   * @param proposal Proposed proposal.
   * @return Failure on error, loops indefinitely otherwise.
   */
  def consensus(proposal: Proposal): Future[Unit] = {
    // Prepare the proposal on a quorum of beakers.
    this.cluster.quorum(_.prepare(proposal)) flatMap { promises =>
      if (promises.map(_.ballot).max > proposal.ballot) {
        // If a promise has been made to a newer proposal, then retry with a higher ballot.
        Thread.sleep(backoff.toMillis)
        val newest = this.round.updateAndGet(i => (i max promises.map(_.ballot).max.round) + 1)
        val ballot = new Ballot(newest, this.id)
        consensus(proposal.setBallot(ballot))
      } else {
        // Otherwise, merge the returned promises into a single proposal.
        val promise = promises.reduce[Proposal](merge).setBallot(proposal.ballot)
        if (!matches(proposal, proposal)) {
          // If the promise does not match the proposal, then retry with the promise.
          Thread.sleep(backoff.toMillis)
          val newest = this.round.getAndIncrement()
          val ballot = new Ballot(newest, this.id)
          consensus(promise.setBallot(ballot)).filter(x => x == x)
        } else {
          // Otherwise, get the keys that read by the promise from a quorum of beakers.
          val depends = promise.commits.asScala.flatMap(_.depends.asScala.keySet)
          this.cluster.quorum(_.get(depends.toSet)) filter { replicas =>
            // Determine the latest and the oldest version of each key.
            val latest = replicas.reduce(merge(_, _))
            val oldest = replicas.reduce(merge(_, _)(revisionOrdering.reverse))

            // Discard all transactions in the promise that cannot be committed.
            val snapshot = Local.Database(latest)
            promise.commits.removeIf(snapshot.commit(_).isFailure)

            // Set the repairs of the promise to the latest revisions of keys that are read - but
            // not written - by the promise for which the beakers disagree on their version.
            latest collect { case (k, v) if oldest(k) < v => promise.repairs.putToChanges(k, v) }

            // Filter promises that do not contain any transactions or repairs.
            !promise.commits.isEmpty || !promise.repairs.changes.isEmpty
          } flatMap { _ =>
            // Otherwise, send the promise to a quorum of beakers and retry automatically.
            this.cluster.quorum(_.accept(promise))
          } flatMap { votes =>
            if (votes.forall(identity)) {
              // If a majority of acceptors vote for the proposal, then share it with a majority.
              learn(promise, null)
              this.cluster.broadcast(_.share(promise))
              Future.unit
            } else {
              // Otherwise, retry with the promise.
              Thread.sleep(backoff.toMillis)
              consensus(promise)
            }
          }
        }
      }
    }
  }

  override def get(
    keys: util.Set[Key],
    handler: AsyncMethodCallback[util.Map[Key, Revision]]
  ): Unit = {
    // Performs a read-only transaction on the underlying database.
    val depends = keys.asScala.map(_ -> long2Long(0L)).toMap
    val readOnly = new Transaction(depends.asJava, new util.HashMap)
    this.scheduler.submit(readOnly)(_ => this.database.read(depends.keySet)) onComplete {
      case Success(r) => handler.onComplete(r.asJava)
      case Failure(_) => handler.onComplete(new util.HashMap)
    }
  }

  override def cas(
    depends: util.Map[Key, Version],
    changes: util.Map[Key, Value],
    handler: AsyncMethodCallback[lang.Boolean]
  ): Unit = synchronized {
    // Changes implicitly depend on the initial version if no dependency is specified.
    val rset = depends.asScala ++ changes.asScala.keySet.map(k => k -> depends.getOrDefault(k, 0L))
    val wset = changes.asScala map { case (k, v) => k -> new Revision(rset(k) + 1, v) }

    // Constructs a proposal containing only the dependencies and changes.
    val ballot = new Ballot(this.round.getAndIncrement(), this.id)
    val transaction = new Transaction(rset.asJava, wset.asJava)
    val proposal = new Proposal(ballot, Set(transaction).asJava, new Transaction())

    // Asynchronously attempt to reach consensus on the proposal.
    consensus(proposal) onComplete {
      case Success(_) => handler.onComplete(true)
      case Failure(_) => handler.onComplete(false)
    }
  }

  override def prepare(
    proposal: Proposal,
    handler: AsyncMethodCallback[Proposal]
  ): Unit = synchronized {
    this.promised.find(_ |> proposal) match {
      case Some(r) =>
        // If a promise has been made to a newer proposal, its ballot is returned.
        handler.onComplete(new Proposal().setBallot(r.ballot))
      case None =>
        // Otherwise, the beaker promises not to accept any proposal that conflicts with the
        // proposal it returns that has a lower ballot than the proposal it receives. If a beaker
        // has already accepted older proposals, it merges them together and returns the result.
        // Otherwise, it returns the proposal with the zero ballot.
        val accept = this.accepted.filter(_ <| proposal)
        val promise = accept.reduceOption[Proposal](merge).getOrElse(proposal.setBallot(new Ballot()))
        this.promised --= this.promised.filter(_ <| proposal)
        this.promised += promise.setBallot(proposal.ballot)
        handler.onComplete(promise)
    }
  }

  override def accept(
    proposal: Proposal,
    handler: AsyncMethodCallback[lang.Boolean]
  ): Unit = synchronized {
    if (this.promised.exists(p => p.ballot > proposal.ballot && (p |> proposal))) {
      // If the beaker has made a promise to a newer proposal, it rejects the proposal.
      handler.onComplete(false)
    } else {
      // Otherwise, the beaker accepts the proposal and discards all older accepted proposals.
      this.accepted --= this.accepted.filter(_ <| proposal)
      this.accepted += proposal
      handler.onComplete(true)
    }
  }

  override def share(
    proposal: Proposal,
    handler: AsyncMethodCallback[Void]
  ): Unit = synchronized {
    // When a beaker learns a proposal, it performs it on all beakers.
    this.cluster.broadcast(_.learn(proposal))
  }

  override def learn(
    proposal: Proposal,
    handler: AsyncMethodCallback[Void]
  ): Unit = synchronized {
    if (!this.learned.happened(proposal.ballot)) {
      // If the proposal has not recently happened, then commit its transactions and repairs on the
      // database and discard all older accepted proposals. Because proposals are idempotent, the
      // same proposal may be safely committed multiple times. However, this is a waste of system
      // resources and should be avoided whenever possible.
      val transactions = proposal.commits.asScala + proposal.repairs
      transactions.foreach(this.scheduler.submit(_)(this.database.commit))
      this.accepted.retain(p => !(p <| proposal))
      this.learned.occurred(proposal.ballot)
    }
  }

  override def close(): Unit = {
    this.database.close()
    this.scheduler.close()
  }

}

object Beaker {

  /**
   * A Beaker server.
   *
   * @param address Network location.
   * @param database Underlying database..
   * @param cluster Beaker cluster.
   */
  case class Server(
    address: Address,
    database: Database,
    cluster: Cluster[Internal.Client]
  ) extends caustic.cluster.Server {

    val beaker     = Beaker(address.hashCode(), this.database, Scheduler(), this.cluster)
    val processor  = new thrift.Beaker.AsyncProcessor(this.beaker)
    val underlying = Announcer(cluster, Thrift.Server(address, this.processor))

    override def serve(): Unit = {
      this.underlying.serve()
    }

    override def close(): Unit = {
      this.underlying.close()
      this.beaker.close()
    }

  }

}