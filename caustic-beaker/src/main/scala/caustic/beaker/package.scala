package caustic

import caustic.beaker.ordering._
import caustic.beaker.thrift.{Ballot, Proposal, Revision, Transaction}

import java.util.Collections._
import scala.collection.JavaConverters._
import scala.math.Ordering.Implicits._

package object beaker {

  //
  type Key = String
  type Version = java.lang.Long
  type Value = String

  // Ballots are totally ordered by their (round, id).
  implicit val ballotOrdering: Ordering[Ballot] = (x, y) => {
    if (x.round == y.round) x.id compare y.id else x.round compare y.round
  }

  // Revisions are uniquely identified and totally ordered by their version.
  implicit val revisionOrdering: Ordering[Revision] = (x, y) => {
    x.version compare y.version
  }

  // Proposals are ordered by their ballot number whenever their transactions are conflict.
  implicit val proposalOrder: Order[Proposal] = (x, y) => {
    x.commits.asScala.find(t => y.commits.asScala.exists(_ ~ t)).map(_ => x.ballot <= y.ballot)
  }

  // Transactions are conflict if either reads or writes a key that the other writes.
  implicit val transactionRelation: Relation[Transaction] = (x, y) => {
    val (xr, xw) = (x.depends.keySet(), x.changes.keySet())
    val (yr, yw) = (y.depends.keySet(), y.changes.keySet())
    !disjoint(xr, yw) || !disjoint(yr, xw) || !disjoint(xw, yw)
  }

  /**
   *
   * @param x
   * @param y
   * @return
   */
  def matches(x: Proposal, y: Proposal): Boolean =
    x.commits == y.commits

  /**
   *
   * @param x
   * @param y
   * @return
   */
  def merge(x: Transaction, y: Transaction): Transaction = {
    val merged = new Transaction(x)
    y.depends.forEach(x.putToDepends(_, _))
    y.changes.forEach(x.putToChanges(_, _))
    merged
  }

  /**
   * Merges the older [[Proposal]] into the newer [[Proposal]]. Each [[Transaction]] in the older
   * [[Proposal]] that conflicts with any [[Transaction]] in the newer [[Proposal]] is discarded.
   *
   * @param x A [[Proposal]].
   * @param y Another [[Proposal]].
   * @return [[Proposal]] union.
   */
  def merge(x: Proposal, y: Proposal): Proposal = {
    val (latest, oldest) = if (x <| y) (y, x) else (x, y)
    val filter = oldest.commits.asScala.filterNot(t => latest.commits.asScala.exists(_ ~ t))
    val merged = new Proposal(latest)
    filter.foreach(merged.addToCommits)
    merged.repairs = merge(latest.repairs, oldest.repairs)
    merged
  }

  /**
   * Merges two maps together. If both maps contain the same key, then the larger value is chosen.
   *
   * @param x A [[Map]].
   * @param y Another [[Map]].
   * @return [[Map]] union.
   */
  def merge[A, B](x: Map[A, B], y: Map[A, B])(implicit ordering: Ordering[B]): Map[A, B] = {
    x ++ y map { case (k, v) => k -> (x.getOrElse(k, v) max v) }
  }

}
