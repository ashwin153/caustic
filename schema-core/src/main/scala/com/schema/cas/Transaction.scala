package com.schema.cas

/**
 * An attempted transaction.
 *
 * @param dependsOn Expected version.
 * @param instructions Instructions to be applied.
 */
case class Transaction(
  dependsOn: Long,
  instructions: Map[String, Instruction]
) {

  /**
   * Whether or not application of the transaction causes a conflict given the specified version
   * mapping. A transaction causes a conflict if any of its instructions depend on an earlier
   * version of an identifier than appears in the version mapping. Transaction conflict detection is
   * deterministic.
   *
   * @param versions Known versions of object identifiers.
   * @return True if transaction conflicts, false otherwise.
   */
  def conflicts(versions: Map[String, Long]): Boolean =
    this.instructions exists { case (id, _) => versions.get(id).fold(false)(this.dependsOn < _) }

}