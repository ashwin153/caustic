package caustic.syntax

import caustic.runtime.thrift.Database

import java.io.InputStream
import org.antlr.v4.runtime.{CharStream, CharStreams}
import scala.concurrent.Awaitable
import scala.io.Source

/**
 *
 */
trait Server {

  /**
   *
   * @param caustic Caustic source code.
   * @param storage Underlying storage system.
   * @return
   */
  def serve(caustic: CharStream, storage: Database.AsyncClient): Awaitable[Unit]

  final def serve(code: String, storage: Database.AsyncClient): Awaitable[Unit] =
    serve(CharStreams.fromString(code), storage)

  final def serve(stream: InputStream, storage: Database.AsyncClient): Awaitable[Unit] =
    serve(CharStreams.fromStream(stream), storage)

  final def serve(source: Source, storage: Database.AsyncClient): Awaitable[Unit] =
    try serve(source.mkString, storage) finally source.close()

}
