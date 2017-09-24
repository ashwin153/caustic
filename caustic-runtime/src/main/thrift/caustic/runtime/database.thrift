namespace * caustic.runtime.thrift

include "transaction.thrift"

/**
 * A retryable failure that is thrown when a database cannot write a set of revisions.
 */
exception ConflictException {
  1: required set<string> keys,
}

/**
 * An unretryable execution failure that is thrown when a transaction is illegally constructed.
 */
exception ExecutionException {
  1: required string message,
}

/**
 * A transactional key-value store.
 */
service Database {

  /**
   * Executes the specified transaction on the underlying database and returns the result. Execute
   * automatically batches reads, and stages writes to a local change buffer that is conditionally
   * put into the underlying database at the end of execution.
   *
   * @param transaction Transaction to execute.
   * @return Result of transaction execution, or an exception on failure.
   */
  transaction.Literal execute(
    1: transaction.Transaction transaction,
  ) throws (
    1: ConflictException conflicts,
    2: ExecutionException execute,
  ),

}
