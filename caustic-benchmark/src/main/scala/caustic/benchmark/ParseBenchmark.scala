package caustic.benchmark

import caustic.runtime.thrift
import caustic.runtime.Transaction
import caustic.runtime.service._

import org.scalameter.api._

object ParseBenchmark extends Bench.OfflineReport {

  // Benchmark transactions containing sequential reads.
  val transactions: Gen[thrift.Transaction] = Gen
    .exponential("size")(2, 1 << 10, 2)
    .map(size => Seq.fill(size)(read(text("x"))).reduce((a, b) => cons(a, b)))

  performance of "Database" in {
    measure method "execute" in {
      using(this.transactions) curve "Execute Latency" in { txn =>
        Transaction.parse(txn)
      }
    }
  }

}
