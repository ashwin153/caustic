namespace * caustic.beaker.thrift

//
typedef string Key
typedef i64 Version
typedef string Value

/**
 *
 * @param version
 * @param value
 */
struct Revision {
  1: Version version,
  2: Value value
}

/**
 *
 */
struct Transaction {
  1: map<Key, Version> depends,
  2: map<Key, Revision> changes,
}

/**
 * A monotonically increasing, unique sequence number.
 *
 * @param round
 * @param id
 */
struct Ballot {
  1: i32 round,
  2: i32 id
}

/**
 *
 * @param ballot
 * @param group
 */
struct Proposal {
  1: Ballot ballot,
  2: set<Transaction> group,
}

/**
 * A distributed, transactional key-value store.
 */
service Beaker {

  /**
   * Returns the latest revisions of the specified keys.
   *
   * @param keys
   * @return
   */
  map<Key, Revision> get(1: set<Key> keys),

  /**
   *
   * @param depends
   * @param changes
   * @return
   */
  bool cas(1: map<Key, Version> depends, 2: map<Key, Value> changes),

  /**
   *
   * @param proposal
   * @return
   */
  Proposal prepare(1: Proposal proposal),

  /**
   *
   * @param proposal
   * @return
   */
  oneway void accept(1: Proposal proposal),

  /**
   *
   * @param proposal
   */
  oneway void learn(1: Proposal proposal),

}