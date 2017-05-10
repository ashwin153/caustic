package com.schema.benchmark

import org.scalameter.api._
import com.schema.runtime._
import org.scalameter.picklers.Implicits._
import com.schema.runtime.local.SynchronizedDatabase
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object DatabaseBenchmark extends Bench.OfflineReport {

  val database: SynchronizedDatabase = SynchronizedDatabase.empty

  override lazy val executor = LocalExecutor(
    new Executor.Warmer.Default,
    Aggregator.min[Double],
    measurer
  )

  // Benchmark transactions containing sequential reads.
  val sequential: Gen[Transaction] = Gen
    .exponential("size")(2, 1024, 2)
    .map(size => Seq.fill(size)(add("1", "2")).reduce((a, b) => cons(a, b)))

  // Benchmark transactions containing nested reads.
  val nested: Gen[Transaction] = Gen
    .exponential("size")(2, 1024, 2)
    .map(depth => Seq.fill(depth)(add("1", "2")).reduce((a, b) => add(a, b)))

  performance of "Database" in {
    measure method "execute" in {
      using(nested) curve "Transaction Depth" in { txn =>
        Await.result(database.execute(txn), Duration.Inf)
      }

      using(sequential) curve "Transaction Size" in { txn =>
        Await.result(database.execute(txn), Duration.Inf)
      }
    }
  }

}