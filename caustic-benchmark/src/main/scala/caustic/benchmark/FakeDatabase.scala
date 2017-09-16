package caustic.benchmark

import caustic.runtime._
import scala.concurrent.{ExecutionContext, Future}

/**
 * An empty, serializable database. Useful to isolate the performance of transaction execution from
 * the cost of performing I/O with a database. Serializability is necessary for compatibility with
 * ScalaMeter, which runs the benchmarks in a separate process.
 */
@SerialVersionUID(1)
object FakeDatabase extends Database with Serializable {

  override def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, Revision]] = Future(Map.empty)

  override def cput(depends: Map[Key, Version], changes: Map[Key, Revision])(
    implicit ec: ExecutionContext
  ): Future[Unit] = Future.unit

}