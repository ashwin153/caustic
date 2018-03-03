package caustic.beaker

import caustic.beaker.concurrent.Executor
import caustic.beaker.storage.Local
import caustic.beaker.Cluster
import org.scalatest.FunSuite
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class BeakerTest extends FunSuite {

  test("End to end") {
    val beaker = Beaker(0, Local.Database(), Executor(), Cluster(Beaker.Client.apply))
    println(Await.result(beaker.propose(Transaction(Map("a" -> 0L), Map("a" -> Revision(1, Array.empty)))), Duration.Inf))
    println(Await.result(beaker.propose(Transaction(Map("a" -> 0L), Map("a" -> Revision(1, Array.empty)))), Duration.Inf))
  }

}
