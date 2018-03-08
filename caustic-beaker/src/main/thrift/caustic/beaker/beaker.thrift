namespace * caustic.beaker.thrift

typedef string Key
typedef i64 Version
typedef string Value

/**
 * A revision is a versioned value. Revisions are uniquely identified and totally ordered by their
 * version. Revisions are monotonic; if a transaction changes a key for which there exists a newer
 * revision, the modification is discarded.
 *
 * @param version Version number.
 * @param value Value.
 */
struct Revision {
  1: Version version,
  2: Value value
}

/**
 * A transaction depends on the versions of a set of keys, called it readset, and changes the values
 * of a set of keys, called its writeset. Defaults to an empty transaction.
 *
 * @param depends Dependencies.
 * @param changes Changes to apply.
 */
struct Transaction {
  1: map<Key, Version> depends = [],
  2: map<Key, Revision> changes = [],
}

/**
 * A monotonically increasing, unique sequence number.
 *
 * @param round Locally unique round number.
 * @param id Globally unique beaker identifier.
 */
struct Ballot {
  1: i32 round = 0,
  2: i32 id = 0
}

/**
 * A proposal commits a set of non-conflicting transactions and repairs a set of keys, and are
 * uniquely identified and totally ordered by their ballot.
 *
 * @param ballot Ballot number.
 * @param commits Transactions to commit.
 * @param repairs Keys to repair.
 */
struct Proposal {
  1: Ballot ballot,
  2: set<Transaction> commits,
  3: Transaction repairs,
}

/**
 * A distributed, transactional key-value store.
 */
service Beaker {

  /**
   * Returns the latest known revision of each key.
   *
   * @param keys Keys to get.
   * @return Revision of each key.
   */
  map<Key, Revision> get(1: set<Key> keys),

  /**
   * Conditionally applies the changes if and only if it depends on the latest versions. Returns
   * whether or not the changes were applied.
   *
   * @param depends Dependencies.
   * @param changes Changes to apply.
   * @return Whether or not the changes were applied.
   */
  bool cas(1: map<Key, Version> depends, 2: map<Key, Value> changes),

  /**
   * Prepares a proposal. If a beaker has not made apromise to a newer proposal, it responds with
   * a promise. When a beaker makes a promise, it refuses to accept any proposal that conflicts
   * with the proposal it returns that has a lower ballot than the proposal it receives. If a
   * beaker has already accepted older proposals, it merges them together and returns the result.
   * Otherwise, it returns the proposal with a zero ballot.
   *
   * @param proposal Proposal to prepare.
   * @return Promise or the ballot of any newer promise that it has made.
   */
  Proposal prepare(1: Proposal proposal),

  /**
   * Accepts a proposal. Beakers accept a proposal if they have not promised not to. If a beaker
   * accepts a proposal, it discards all older accepted proposals and broadcasts a vote for it.
   *
   * @param proposal Proposal to accept.
   */
  oneway void accept(1: Proposal proposal),

  /**
   * Votes for a proposal. Beakers learn a proposal once a majority of beakers vote for it. If a
   * beaker learns a proposal, it commits its transactions and repairs on its replica of the
   * database.
   *
   * @param proposal Proposal to learn.
   */
  oneway void learn(1: Proposal proposal),

}