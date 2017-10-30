package caustic.compiler

import scala.reflect.io.Path

/**
 * An exception handler. https://scala-lang.org/blog/2016/10/14/dotty-errors.html
 *
 * @param file File path.
 * @param source Source code.
 * @param suppress Suppress warnings.
 */
case class Reporter(
  file: Path,
  source: String,
  suppress: Boolean = false,
) {

  /**
   * Prints an error message to the console.
   *
   * @param line Line number.
   * @param columns Effected columns.
   * @param error: Error.
   */
  def error(line: Int, columns: Range, error: Error): Unit =
    println(
      s"""
         |-- [E${ "%3d".format(error.code) }] ${ error.name }: ${ file.parent.name }/${ file.name }
         |${ "%4d".format(line) } | ${ source.split("\n")(line) }
         |      | ${ " " * columns.start }${ "^" * columns.size }
         |      | ${ error.description.grouped(100).mkString("\n      | ")}
       """.stripMargin
    )

  /**
   * Prints a warning message to the console.
   *
   * @param line Line number.
   * @param columns Effected columns.
   * @param warning Warning.
   */
  def warning(line: Int, columns: Range, warning: Warning): Unit =
    if (!suppress) println(
      s"""
         |-- [W${ "%3d".format(warning.code) }] ${ warning.name }: ${ file.parent.name }/${ file.name }
         |${ "%4d".format(line) } | ${ source.split("\n")(line) }
         |      | ${ " " * columns.start }${ "^" * columns.size }
         |      | ${ warning.description.grouped(100).mkString("\n      | ")}
       """.stripMargin
    )

}