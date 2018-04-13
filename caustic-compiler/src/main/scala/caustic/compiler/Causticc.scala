package caustic.compiler

import caustic.compiler.parsing.Generate
import caustic.compiler.typing.Universe
import caustic.grammar.{CausticLexer, CausticParser}

import org.antlr.v4.runtime._

import java.nio.file.{Files, Path, Paths}
import org.antlr.v4.runtime.misc.Interval
import scala.util.{Failure, Try}
import scala.collection.mutable

/**
 * A compiler for the Caustic programming language.
 */
object Causticc {

  def main(args: Array[String]): Unit = {
    // Print usage information.
    if (args.length < 1 || args.length > 2) {
      println("Usage: causticc <source> [config]")
      System.exit(1)
    }

    // Run the compiler on all the specified files.
    Files.walk(Paths.get(args(0))) forEach { path =>
      if (path.toString.endsWith(".acid")) Causticc(path).get
    }
  }

  /**
   * Compiles the specified source stream and returns the generated sources.
   *
   * @param source Source stream.
   * @return Generated sources.
   */
  def apply(source: CharStream): Try[String] = {
    // Lex and parse the program.
    val lexer = new CausticLexer(source)
    val tokens = new CommonTokenStream(lexer)
    val parser = new CausticParser(tokens)

    // Register the custom error handler.
    val handler = Handler(
      source.getSourceName,
      source.getText(Interval.of(0, source.size())) split "\n"
    )

    parser.removeErrorListeners()
    parser.addErrorListener(handler)

    Try {
      // Run code generation and return the result.
      val gen = Generate(Universe.root)
      val out = gen.visitProgram(parser.program())
      out
    } recoverWith { case e: Error =>
      // Report any errors during code generation.
      handler.report(e)
      Failure(e)
    }
  }

  /**
   * Compiles the specified source file and returns the generated sources. Compilation is memoized;
   * therefore, the same path will never be recompiled twice, even if it is imported in multiple
   * different files. Incremental compilation can be implemented by persisting this information
   * across compiler invocations. https://stackoverflow.com/a/36960228/1447029
   */
  lazy val apply: Path => Try[String] = new mutable.HashMap[Path, Try[String]]() {
    override def apply(path: Path): Try[String] = getOrElseUpdate(path, {
      val source = if (path.isAbsolute) path else Paths.get("").toAbsolutePath.resolve(path)
      Causticc(CharStreams.fromPath(source))
    })
  }

  /**
   * Compiles the specified source code and returns the generated sources.
   *
   * @param source Source code.
   * @return Generated sources.
   */
  def apply(source: String): Try[String] = Causticc(CharStreams.fromString(source))

}