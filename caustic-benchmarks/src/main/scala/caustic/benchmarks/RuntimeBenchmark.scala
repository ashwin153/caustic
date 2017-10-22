package caustic.benchmarks

import caustic.runtime
import caustic.runtime.local.LocalDatabase
import caustic.runtime.thrift
import caustic.runtime.service
import org.scalameter.api._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

object RuntimeBenchmark extends Bench.OfflineReport {

  // Fake database to "mock" database performance.
  val database: runtime.Database = LocalDatabase()

  // Benchmark runtime transactions containing sequential reads.
  val runtimeTransactions: Gen[runtime.Transaction] = Gen
    .exponential("size")(2, 1 << 10, 2)
    .map(size => Seq.fill(size)(runtime.read(runtime.text("x"))).reduce(runtime.cons))

  // Benchmark thrift transactions containing sequential reads.
  val thriftTransactions: Gen[thrift.Transaction] = Gen
    .exponential("size")(2, 1 << 10, 2)
    .map(size => Seq.fill(size)(service.read(service.text("x"))).reduce(service.cons))

  performance of "Runtime" in {
    measure method "Database.execute" in {
      using(this.runtimeTransactions) curve "Execute Latency" in { txn =>
        Await.result(this.database.execute(txn), Duration.Inf)
      }
    }

    measure method "Transaction.parse" in {
      using(this.thriftTransactions) curve "Parse Latency" in { runtime.Transaction.parse }
    }
  }

}
