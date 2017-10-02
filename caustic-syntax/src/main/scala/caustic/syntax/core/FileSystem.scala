package caustic.syntax
package core

import caustic.runtime.thrift

/**
 *
 * @param index
 */
case class FileSystem(index: Index) {

  /**
   *
   * @param path
   * @return
   */
  def lookup(path: Path): thrift.Transaction =
    index.get(path.flatten)

  /**
   *
   * @param path
   * @return
   */
  def exists(path: Path): thrift.Transaction =
    equal(lookup(path.kind), Empty)

  /**
   *
   * @param path
   * @return
   */
  def mkdir(path: Path): thrift.Transaction =
    cons(
      // All paths in the path hierarchy are directories.
      path.hierarchy
        .map(p => branch(notEqual(add(p, "/$kind"), "dir"), cons(remove(p), write(add(p, "/$kind"), "dir"))))
        .foldLeft(Empty)((a, b) => cons(a, b)),

      // All directories contain the names of their contents.
      path.hierarchy.sliding(2)
        .map { case Seq(p, c) => branch(not(contains(p, c.name)), write(p, add(read(p), c.name, ","))) }
        .foldLeft(Empty)((a, b) => cons(a, b)),
    )

  def write()
  /**
   *
   * @param path
   * @param name
   * @return
   */
  def create(path: Path, name: thrift.Transaction): thrift.Transaction =
    branch(not(contains(lookup(path), name)), cons(
      index.set(path.flatten, add(lookup(path), ",", name)),
      delete(path),

    ))

  /**
   *
   * @param path
   * @return
   */
  def remove(path: Path): thrift.Transaction = {
    scan(path) { key =>
      index.set(key, Empty)
      index.set(add(key, "/$kind"), Empty)
    }
  }

  /**
   *
   * @param src
   * @param dest
   * @return
   */
  def copy(src: Path, dest: Path): thrift.Transaction =
    scan(src) { key =>
      index.set(add(dest.flatten, slice(key, length(src.flatten))), index.get(key))
      index.set(add(dest.flatten, slice(add(key, "/$kind"), length(src.flatten))), index.get(add(key, "/$kind")))
    }

  /**
   *
   * @param path
   * @return
   */
  def update(path: Path, value: thrift.Transaction): thrift.Transaction =
    cons(
      // Verify the integrity of the path hierarchy.
      path.hierarchy.sliding(2)
        .map { case Seq(p, c) => cons(mkdir(p), create(p, c.name)) }
        .foldLeft(Empty)((a, b) => cons(a, b)),

      // Ensure that the path corresponds to a file.
      branch(not(equal(lookup(path.kind), "F")), cons(
        delete(path),
        index.set(path.kind.flatten, "F")
      )),

      // Update the contents of the path.
      index.set(path.flatten, value)
    )

  /**
   *
   * @param path
   * @param f
   * @return
   */
  def scan(path: Path)(f: thrift.Transaction => thrift.Transaction): thrift.Transaction = {
    cons(
      store("$stack", path.flatten),
      repeat(notEqual(length(load("$stack")), Zero), cons(
        // Pop the head of the stack.
        store("$i", indexOf(load("$stack"), ",")),
        store("$head", slice(load("$stack"), Zero, load("$i"))),
        store("$stack", slice(load("$stack"), add(load("$i"), One))),

        // Recurse on directories.
        branch(equal(index.get(add(load("$head"), "/$kind")), "D"), cons(
          store("$fields", read(load("$head"))),
          store("$i", Zero),

          // Prefix field names with the key.
          repeat(lessThan(load("$i"), length(load("$fields"))), cons(
            store("$j", indexOf(slice(load("$fields"), load("$i")), ",")),
            store("$prefix", slice(load("$fields"), Zero, load("$i"))),
            store("$suffix", slice(load("$fields"), load("$j"))),
            store("$name", slice(load("$fields"), load("$i"), load("$j"))),
            store("$key", add(load("$head"), "/", load("$name"))),
            store("$fields", add(load("$prefix"), load("$key"), load("$suffix"))),
            store("$i", add(load("$i"), length(load("$head")), Two))
          )),

          // Prefetch all fields of the head.
          prefetch(load("$fields")),
          store("$stack", add(load("$stack"), load("$fields"), ","))
        )),

        // Apply the function to the head key.
        f(load("$head"))
      ))
    )
  }

}