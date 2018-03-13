package caustic.beaker.common

import scala.reflect.ClassTag

/**
 * A collection of events. Histories remember a finite number of previous events, which may be used
 * to prevent the same events from occurring again. Histories are implemented as circular arrays.
 *
 * @param buffer Underlying array.
 */
class History[T](buffer: Array[T]) {

  private[this] var cursor: Int = 0

  /**
   * Returns whether or not the event has recently occurred.
   *
   * @param event Event that may have occurred.
   * @return Whether or not the event recently occurred.
   */
  def happened(event: T): Boolean = {
    this.buffer.contains(event)
  }

  /**
   * Adds the event to the history.
   *
   * @param event Event that occurred.
   */
  def occurred(event: T): Unit = {
    this.buffer(this.cursor) = event
    this.cursor = (this.cursor + 1) % this.buffer.length
  }

}

object History {

  /**
   * Constructs a history of the specified size.
   *
   * @param size History length.
   * @return Empty history.
   */
  def apply[T: ClassTag](size: Int): History[T] = new History(Array.ofDim[T](size))

}
