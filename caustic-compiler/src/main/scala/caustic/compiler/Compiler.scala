package caustic.compiler

import caustic.compiler.parsing.Generate
import caustic.compiler.typing.Universe
import caustic.grammar.{CausticLexer, CausticParser}
import org.antlr.v4.runtime.{CharStreams, CommonTokenStream}
import java.io.{File, FileOutputStream}
import java.nio.file.{Files, Path, Paths}
import scala.util.{Failure, Try}
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.io.Source

/**
 * A compiler for the Caustic programming language.
 */
object Compiler {

  /**
   *
   * @param args
   */
  def main(args: Array[String]): Unit = {
    // Print usage information.
    if (args.length < 1 || args.length > 2) {
      println("Usage: causticc <source> [config]")
      System.exit(1)
    }

    // Run the compiler on all the specified files.
    Files.walk(Paths.get(args(0))) forEach { path =>
      if (path.toString.endsWith(".acid")) Compiler.compile(path).get
    }
  }

  /**
   * Compiles the specified source file, and returns the generated universe. Compilation is
   * memoized (https://stackoverflow.com/a/36960228/1447029); therefore, the same path will never be
   * recompiled twice, even if it is imported in multiple different files. Incremental compilation
   * can be implemented by persisting the map across compiler invocations.
   */
  lazy val compile: Path => Try[Universe] = new mutable.HashMap[Path, Try[Universe]]() {
    override def apply(path: Path): Try[Universe] = getOrElseUpdate(path, {
      // Resolve the path into an absolute path.
      val source = if (path.isAbsolute) path else Paths.get("").toAbsolutePath.resolve(path)

      // Lex and parse the program.
      val lexer = new CausticLexer(CharStreams.fromPath(source))
      val tokens = new CommonTokenStream(lexer)
      val parser = new CausticParser(tokens)

      // Compile the program.
      compile(parser, Handler(source, Source.fromFile(source.toFile).getLines().toSeq))
    })
  }

  /**
   * Compiles the specified program, and returns the generated universe..
   *
   * @param parser  ANTLR parser.
   * @param handler Error handler.
   * @return Generated universe.
   */
  def compile(parser: CausticParser, handler: Handler): Try[Universe] =
    Try {
      // Run code generation on the parsed program.
      parser.removeErrorListeners()
      parser.addErrorListener(handler)
      val generator = Generate(Universe.root)
      val output = generator.visitProgram(parser.program())

      // Write the generated sources to file.
      val file = new File(parser.getSourceName.replaceFirst(".acid$", "") + ".scala")
      val writer = new FileOutputStream(file)
      writer.write(output.getBytes("UTF-8"))
      writer.close()

      // Return the generated universe.
      generator.universe
    } recoverWith { case e: Error =>
      // Report any errors during compilation.
      handler.report(e)
      Failure(e)
    }

}