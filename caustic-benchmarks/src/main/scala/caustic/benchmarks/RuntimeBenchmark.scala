package caustic.benchmarks

import caustic.runtime
import caustic.runtime.{Transaction, thrift}
import caustic.service.client

import org.scalameter.api._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

object RuntimeBenchmark extends Bench.OfflineReport {

  // Fake database to "mock" database performance.
  val database: runtime.Database = FakeDatabase

  // Benchmark runtime transactions containing sequential reads.
  val RTransactions: Gen[runtime.Transaction] = Gen
    .exponential("size")(2, 1 << 10, 2)
    .map(size => Seq.fill(size)(runtime.read(runtime.text("x"))).reduce(runtime.cons))

  // Benchmark thrift transactions containing sequential reads.
  val TTransactions: Gen[thrift.Transaction] = Gen
    .exponential("size")(2, 1 << 10, 2)
    .map(size => Seq.fill(size)(client.read(client.text("x"))).reduce(client.cons))

  performance of "Runtime" in {
    measure method "Database.execute" in {
      using(this.RTransactions) curve "Execute Latency" in { txn =>
        Await.result(this.database.execute(txn), Duration.Inf)
      }
    }

    measure method "Transaction.parse" in {
      using(this.TTransactions) curve "Parse Latency" in { Transaction.parse }
    }
  }

}
