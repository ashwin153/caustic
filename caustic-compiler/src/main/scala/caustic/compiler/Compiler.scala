package caustic.compiler

import caustic.compiler.types.Universe
import java.nio.file.Paths

/**
 *
 */
object Compiler extends App {

  if (args.length < 2) {
    // Print usage information.
    println(
      """
        |Usage: cc <query> <file>
        |
        |Queries:
        |  - check: Runs the type checker.
        |  - compile: Compiles a program.
        |  - repl: Opens a REPL session.
        |  - run: Executes a program.
      """.stripMargin
    )

    // Fail fast.
    System.exit(1)
  }

  // Load and execute query.
  val query = args(0) match {
    case "simplify" => goals.Simplify(Universe.root)
    case "declare" => goals.Declare(Universe.root)
    case "gen" => goals.Gen(Universe.root)
  }

  println(this.query.execute(Paths.get(args(1))))

}