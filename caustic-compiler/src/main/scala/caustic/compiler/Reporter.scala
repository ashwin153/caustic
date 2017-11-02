//package caustic.compiler
//
//import java.nio.file.Path
//import org.antlr.v4.runtime._
//
///**
// * An exception handler. https://scala-lang.org/blog/2016/10/14/dotty-errors.html
// *
// * @param path File path.
// * @param lines Source code.
// */
//case class Reporter(
//  path: Path,
//  lines: Seq[String]
//) extends BaseErrorListener {
//
//  /**
//   * Prints an error message to the console.
//   *
//   * @param line Line number.
//   * @param columns Effected columns.
//   * @param error: Error.
//   */
//  def error(line: Int, columns: Range, error: Error): Unit = {
//    println(
//      s"""
//         |${ Console.RED }[E${ "%03d".format(error.code) }] ${ error.name }: ${ path.getParent.getFileName }/${ path.getFileName } ${ Console.RESET }
//         |${ Console.CYAN }${ "-" * 100 }${ Console.RESET }
//         |${ Console.CYAN } ${ "%4d".format(line) }  |${ Console.RESET } ${ lines(line - 1) }
//         |${ Console.CYAN }       |${ Console.RESET } ${ " " * columns.start }${ Console.CYAN }${ "^" * columns.size }${ Console.RESET }
//         |${ Console.CYAN }       |${ Console.RESET } ${ error.description.grouped(91).mkString(s"\n|       ${ Console.CYAN }|${ Console.RESET } ") }
//       """.stripMargin
//    )
//  }
//
//  /**
//   * Prints a warning message to the console.
//   *
//   * @param line Line number.
//   * @param columns Effected columns.
//   * @param warning Warning.
//   */
//  def warning(line: Int, columns: Range, warning: Warning): Unit =
//    println(
//      s"""
//         |${ Console.YELLOW }[W${ "%03d".format(warning.code) }] ${ warning.name }: ${ path.getParent.getFileName }/${ path.getFileName } ${ Console.RESET }
//         |${ Console.CYAN}${ "-" * 100 }${ Console.RESET }
//         |${ Console.CYAN} ${ "%4d".format(line) }  |${ Console.RESET } ${ lines(line - 1) }
//         |${ Console.CYAN}       |${ Console.RESET } ${ " " * columns.start }${ Console.CYAN }${ "^" * columns.size }${ Console.RESET }
//         |${ Console.CYAN}       |${ Console.RESET } ${ warning.description.grouped(91).mkString(s"\n|       ${ Console.CYAN }|${ Console.RESET } ") }
//       """.stripMargin
//    )
//
//  override def syntaxError(
//    recognizer: Recognizer[_, _],
//    offendingSymbol: Any,
//    line: Int,
//    position: Int,
//    message: String,
//    exception: RecognitionException
//  ): Unit = error(line, position until position + 1, Error.syntax(message))
//
//}