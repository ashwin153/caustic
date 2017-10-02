package caustic.syntax
package core

import caustic.runtime.thrift

/**
 *
 * @param parts Path elements.
 */
case class Path(parts: List[thrift.Transaction]) {

  /**
   *
   * @return
   */
  def name: thrift.Transaction =
    if (this.parts.isEmpty) Empty else this.parts.last

  /**
   *
   * @return
   */
  def parent: Option[Path] =
    if (this.parts.isEmpty) None else Some(Path(this.parts.dropRight(1)))

  /**
   *
   * @return
   */
  def kind: Path =
    Path(this.parts :+ text("$kind"))

  /**
   *
   * @return
   */
  def flatten: thrift.Transaction =
    this.parts.foldLeft(Empty)((a, b) => add(a, "/", b))

  /**
   *
   * @return
   */
  def hierarchy: Seq[Path] =
    (1 to this.parts.length).map(i => Path(this.parts.take(i)))

}
