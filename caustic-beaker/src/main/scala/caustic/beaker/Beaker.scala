package caustic.beaker

import caustic.beaker.concurrent._
import caustic.beaker.ordering._
import caustic.beaker.storage.Local
import caustic.beaker.thrift.{Ballot, Proposal, Revision, Transaction}
import caustic.cluster.protocol.Thrift
import caustic.cluster.{Address, Announcer, Cluster}
import org.apache.thrift.async.AsyncMethodCallback
import java.io.Closeable
import java.{lang, util}
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.language.postfixOps
import scala.math.Ordering.Implicits._
import scala.util.{Failure, Random, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * A distributed, transactional key-value store.
 *
 * @param id Unique identifier.
 * @param database Underlying [[Database]].
 * @param executor Transaction [[Executor]].
 * @param cluster Beaker [[Cluster]].
 */
case class Beaker(
  id: Int,
  database: Database,
  executor: Executor[Transaction],
  cluster: Cluster[Internal.Client]
) extends thrift.Beaker.AsyncIface with Closeable {

  var round    : AtomicInteger                  = new AtomicInteger(1)
  val proposed : mutable.Map[Transaction, Task] = mutable.Map.empty
  val promised : mutable.Set[Proposal]          = mutable.Set.empty
  val accepted : mutable.Set[Proposal]          = mutable.Set.empty
  val learned  : mutable.Map[Proposal, Int]     = mutable.Map.empty

  /**
   * Attempts to coordinate consensus on a [[Proposal]]. Uses a variation of Generalized Paxos in
   * https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/tr-2005-33.pdf to reach
   * consensus.
   *
   * @param proposal Proposed [[Proposal]].
   * @return [[Failure]] on error, loops indefinitely otherwise.
   */
  def paxos(proposal: Proposal): Try[Unit] = {
    this.cluster.quorum(_.prepare(proposal)) flatMap { promises =>
      val maximum = promises.map(_.ballot).max
      if (maximum > proposal.ballot) {
        // If any promise has a larger ballot, then retry with a higher ballot.
        Thread.sleep(Random.nextInt(250) + 10000)
        val round = this.round.updateAndGet(i => (i max maximum.round) + 1)
        paxos(proposal.setBallot(new Ballot(round, this.id)))
      } else {
        // Otherwise, merge together all the returned proposals.
        val promise = promises.reduce(union).setBallot(proposal.ballot)
        if (proposal != promise) {
          // If the promise does not match the proposal, then retry with the promise.
          Thread.sleep(Random.nextInt(250) + 10000)
          val next = this.round.getAndIncrement()
          paxos(promise.setBallot(new Ballot(next, this.id)))
        } else {
          // Otherwise, read all the dependencies of all the components of the proposal.
          val depends = promise.group.asScala.flatMap(_.depends.asScala.keySet)
          val changes = promise.group.asScala.flatMap(_.changes.asScala.keySet)

          this.cluster.quorum(_.get(depends.toSet)) map { replicas =>
            // Determine the latest and the oldest version of each key.
            val latest = replicas.reduce(merge(_, _)(revisionOrdering))
            val oldest = replicas.reduce(merge(_, _)(revisionOrdering.reverse))

            // If the latest and oldest versions differ, then write the latest value.
            val repair = latest filter { case (k, v) => oldest(k) < v }
            val update = new Transaction(new util.HashMap, repair.asJava)

            // Remove all components of the proposal that cannot be committed.
            val snapshot = Local.Database(latest)
            val valid = promise.group.asScala.filter(c => snapshot.commit(c).isSuccess)
            if (repair.nonEmpty) valid + update else valid
          } filter {
            // If the proposal is empty, then return.
            _.nonEmpty
          } flatMap { group =>
            // Otherwise, attempt to accept the proposal and retry automatically.
            val accept = promise.setGroup(group.asJava)
            this.cluster.broadcast(_.accept(accept))
            Thread.sleep(Random.nextInt(250) + 10000)
            paxos(accept)
          }
        }
      }
    }
  }

  override def get(keys: util.Set[Key], handler: AsyncMethodCallback[util.Map[Key, Revision]]): Unit = {
    // Performs a read-only transaction on the underlying database. Guarantees that a client will
    // read their own writes, and that reads are monotonic.
    val depends = keys.asScala.map(_ -> long2Long(0L)).toMap
    val readOnly = new Transaction(depends.asJava, new util.HashMap)

    this.executor.submit(readOnly)(_ => this.database.read(depends.keySet)) onComplete {
      case Success(r) => handler.onComplete(r.asJava)
      case Failure(e) => handler.onComplete(new util.HashMap)
    }
  }

  override def cas(depends: util.Map[Key, Version], changes: util.Map[Key, Value], handler: AsyncMethodCallback[lang.Boolean]): Unit = {
    // Changes implicitly depend on the initial version if no dependency is specified.
    val rset = depends.asScala ++ changes.asScala.keySet.map(k => k -> depends.getOrDefault(k, 0L))
    val wset = changes.asScala map { case (k, v) => k -> new thrift.Revision(rset(k) + 1, v) }
    val transaction = new thrift.Transaction(rset.asJava, wset.asJava)

    // Asynchronously attempt to reach consensus on the transaction.
    val ballot = new Ballot(this.round.getAndIncrement(), this.id)
    val daemon = Task(paxos(new Proposal(ballot, Set(transaction).asJava)))
    this.proposed += transaction -> daemon

    daemon.future onComplete {
      case Success(_) => handler.onComplete(true)
      case Failure(_) => handler.onComplete(false)
    }
  }

  override def prepare(proposal: Proposal, handler: AsyncMethodCallback[Proposal]): Unit = {
    this.promised.find(_ |> proposal) match {
      case Some(r) =>
        // If a conflicting proposal with a higher ballot is promised, then its ballot is returned.
        handler.onComplete(new Proposal(r.ballot, new util.HashSet))
      case None =>
        // If conflicting proposals are accepted, then they are merged together, promised, and
        // returned. Otherwise, the original proposal is promised and returned.
        val accept = this.accepted.filter(_ <| proposal)
        val promise = accept.reduceOption(union).getOrElse(proposal.setBallot(new Ballot(0, 0)))
        this.promised --= this.promised.filter(_ <| proposal)
        this.promised += promise.setBallot(proposal.ballot)
        handler.onComplete(promise)
    }
  }

  override def accept(proposal: Proposal, handler: AsyncMethodCallback[Void]): Unit = {
    // Accepts and votes for the proposal if there isn't a newer, conflicting promise.
    if (!this.promised.exists(p => p != proposal && (p |> proposal))) {
      this.accepted --= this.accepted.filter(_ <| proposal)
      this.accepted += proposal
      this.cluster.broadcast(_.learn(proposal))
    }
  }

  override def learn(proposal: Proposal, handler: AsyncMethodCallback[Void]): Unit = {
    // Remove all older learned proposals and vote for the proposal.
    val votes = this.learned.getOrElse(proposal, 0)
    this.learned --= this.learned.keys.filter(_ <| proposal)
    this.learned(proposal) = votes + 1

    // If a majority of the cluster has voted for a proposal, then commit its contents. Once a
    // transaction is learned, consensus on all conflicting proposed transactions completes.
    if (this.learned(proposal) == this.cluster.size / 2 + 1) {
      proposal.group.asScala foreach { t =>
        this.executor.submit(t)(this.database.commit) onComplete { _ =>
          val completed = this.proposed.filterKeys(_ ~ t)
          completed foreach { case (u, v) => if (t == u) v.finish() else v.cancel() }
          this.proposed --= completed.keys
        }
      }
    }
  }

  override def close(): Unit = {
    this.database.close()
    this.executor.close()
  }

}

object Beaker {

  /**
   * A Beaker server.
   *
   * @param address Network location.
   * @param database Underlying [[Database]].
   * @param cluster Beaker [[Cluster]].
   */
  case class Server(
    address: Address,
    database: Database,
    cluster: Cluster[Internal.Client]
  ) extends caustic.cluster.Server {

    val beaker     = Beaker(address.hashCode(), this.database, Executor(), this.cluster)
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