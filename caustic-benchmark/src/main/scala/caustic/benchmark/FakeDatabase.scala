package caustic.benchmark

import caustic.runtime._
import scala.concurrent.{ExecutionContext, Future}

@SerialVersionUID(1)
class FakeDatabase extends Database with Serializable {

  override def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, Revision]] = Future(Map.empty)

  override def put(depends: Map[Key, Version], changes: Map[Key, Revision])(
    implicit ec: ExecutionContext
  ): Future[Unit] = Future.unit

}
