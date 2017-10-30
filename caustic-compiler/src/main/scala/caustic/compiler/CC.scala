package caustic.compiler

import caustic.compiler.check.Check
import caustic.compiler.compile.Compile
import caustic.compiler.format.Format
import caustic.compiler.repl.Repl
import caustic.compiler.run.Run
import java.io.File
import scala.collection.JavaConverters._
import scala.io.Source

/**
 *
 */
object CC extends App {

  // Print usage information.
  if (args.length < 2)
    println
      """
        |Usage: cc <query> <file> [args]
        |
        |Queries:
        |  - check: Runs the type checker.
        |  - compile: Compiles a program.
        |  - format: Runs the linter.
        |  - repl: Opens a REPL session.
        |  - run: Executes a program.
      """.stripMargin

  // Load and execute query.
  val query = args(0) match {
    case "check" => Check
    case "compile" => Compile
    case "format" => Format
    case "repl" => Repl
    case "run" => Run
  }

  val file = new File(args(1))
  println(this.query.execute(Source.fromFile(this.file).getLines.mkString))

}