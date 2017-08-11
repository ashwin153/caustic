package caustic.runtime

import caustic.runtime.parser._

/**
 * A versioned value. Revisions of a key are totally ordered by their associated version. Revisions
 * are the mechanism through which transactional consistency is achieved; if a newer revision exists
 * for a key that is read or written, then a transaction is rejected.
 *
 * @param version Version number.
 * @param value Literal value.
 */
@SerialVersionUID(1L)
case class Revision(
  version: Version,
  value: Literal
) extends Serializable

object Revision {

  // Cache the initial revision, because it is created for each key.
  val Initial: Revision = Revision(0L, text(""))

}