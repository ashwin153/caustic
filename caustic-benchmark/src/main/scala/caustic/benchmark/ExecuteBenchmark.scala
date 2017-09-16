package caustic.benchmark

import caustic.runtime._
import caustic.runtime.interpreter._

import org.scalameter.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

object ExecuteBenchmark extends Bench.OfflineReport {

  // Fake database to "mock" database performance.
  val database: Database = FakeDatabase

  // Benchmark transactions containing sequential reads.
  val transactions: Gen[Transaction] = Gen
    .exponential("size")(2, 1 << 10, 2)
    .map(size => Seq.fill(size)(read(text("x"))).reduce((a, b) => cons(a, b)))

  performance of "Database" in {
    measure method "execute" in {
      using(this.transactions) curve "Execute Latency" in { txn =>
        Await.result(this.database.execute(txn), Duration.Inf)
      }
    }
  }

}
