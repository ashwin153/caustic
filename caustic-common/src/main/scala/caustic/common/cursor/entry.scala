package caustic.common.cursor

/**
 * A log entry. Log entries are associated with a monotonically increasing offset that indicates the
 * relative position of the entry in the log. Each offset is guaranteed to (1) be non-negative, and
 * (2) be larger for entries appended after. Log entries may either be of the type [[Record]], in
 * which case they are associated with some data, or of type [[Pending]], which indicates that no
 * additional records currently exist in the log.
 */
sealed trait Entry[T] { val offset: Long }
case class Pending[T](offset: Long) extends Entry[T]
case class Record[T](offset: Long, payload: T) extends Entry[T]