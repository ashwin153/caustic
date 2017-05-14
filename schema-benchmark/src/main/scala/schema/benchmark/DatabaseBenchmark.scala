package schema.benchmark

import schema.runtime._
import org.scalameter.api._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object DatabaseBenchmark extends Bench.OfflineReport {

  val database: Database = local.SynchronizedDatabase("x" -> "0.0", "y" -> "1.0")

  // Benchmark transactions containing sequential reads.
  val sequential: Gen[Transaction] = Gen
    .exponential("size")(2, 1 << 10, 2)
    .map(size => Seq.fill(size)(read(literal("x"))).reduce((a, b) => cons(a, b)))

  performance of "Database" in {
    measure method "execute" in {
      using(sequential) curve "Transaction Size" in { txn =>
        Await.result(database.execute(txn), Duration.Inf)
      }
    }
  }

}