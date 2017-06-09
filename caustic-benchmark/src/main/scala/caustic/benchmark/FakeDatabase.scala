package caustic.benchmark

import caustic.runtime._
import scala.concurrent.{ExecutionContext, Future}

@SerialVersionUID(1)
class FakeDatabase extends Database with Serializable {

  override def get(keys: Iterable[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, (Revision, Value)]] = Future(Map.empty)

  override def put(depends: Map[Key, Revision], changes: Map[Key, (Revision, Value)])(
    implicit ec: ExecutionContext
  ): Future[Unit] = Future.unit

}
