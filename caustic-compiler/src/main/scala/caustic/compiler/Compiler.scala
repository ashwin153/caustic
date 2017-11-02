package caustic.compiler

import caustic.compiler.types.Universe
import java.nio.file.Paths

/**
 *
 */
object Compiler extends App {

  // Print usage information.
  if (args.length < 2) {
    println(
      """Usage: cc <goal> <file>
        |
        |Goals:
        |  - declare: Evaluates declarations.
        |  - gen-thrift: Generates a Thrift IDL.
        |  - gen-scala: Generates a Scala implementation.
        |  - simplify: Simplifies expressions.
      """.stripMargin
    )

    System.exit(1)
  }

  // Load and execute query.
  val goal = args(0) match {
    case "declare" => goals.Declare(Universe.root)
    case "gen-thrift" => goals.GenThrift
    case "gen-scala" => goals.GenScala
    case "simplify" => goals.Simplify(Universe.root)
  }

  println(this.goal.execute(Paths.get(args(1))))

}