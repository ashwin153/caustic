package caustic.syntax
package core

import caustic.runtime.thrift

/**
 *
 */
trait Index {

  /**
   *
   * @param path
   * @return
   */
  def get(path: Path): thrift.Transaction =
    read(path)

  /**
   *
   * @param path
   * @param value
   * @return
   */
  def set(path: Path, value: thrift.Transaction): thrift.Transaction =
    cons(
      // All paths in the path hierarchy are directories.
      path.hierarchy
        .map(p => branch(notEqual(add(p, "/$kind"), "dir"), cons(remove(p), write(add(p, "/$kind"), "dir"))))
        .foldLeft(Empty)((a, b) => cons(a, b)),

      // All directories contain the names of their contents.
      path.hierarchy.sliding(2)
        .map { case Seq(p, c) => branch(not(contains(p, c.name)), write(p, add(read(p), c.name, ","))) }
        .foldLeft(Empty)((a, b) => cons(a, b)),

      // The path corresponds to a file.
      branch(notEqual(add(path, "/$kind"), "file"), cons(remove(path), write(add(path, "/$kind"), "file"))),
      branch(notEqual(read(path), value), write(path.flatten, value))
    )


  /**
   *
   * @param prefix
   * @return
   */
  def scan(prefix: Path): thrift.Transaction

  /**
   *
   * @param src
   * @param dest
   * @return
   */
  def copy(src: Path, dest: Path): thrift.Transaction

  /**
   *
   * @param path
   * @return
   */
  def remove(path: Path): thrift.Transaction

}



///**
// * A file system index.
// *
// * @param isLocal
// */
//case class Index(isLocal: Boolean) {
//
//  /**
//   *
//   * @param key
//   * @return
//   */
//  def get(key: thrift.Transaction): thrift.Transaction =
//    if (this.isLocal)
//      load(key)
//    else
//      read(key)
//
//  /**
//   *
//   * @param key
//   * @param value
//   * @return
//   */
//  def set(key: thrift.Transaction, value: thrift.Transaction): thrift.Transaction =
//    if (this.isLocal)
//      branch(notEqual(get(key), value), store(key, value))
//    else
//      branch(notEqual(get(key), value), write(key, value))
//
//}
