package com.schema.benchmark

import org.scalameter.api._
import com.schema.runtime._
import com.schema.runtime.local.SynchronizedDatabase
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object DatabaseBenchmark extends Bench.LocalTime {

  val database: SynchronizedDatabase = SynchronizedDatabase.empty

  // Benchmark transactions containing sequential reads.
  val sizes: Gen[Int] = Gen.range("size")(10, 100, 10)
  val sequential: Gen[Transaction] = sizes.map(size => Seq.fill(size)(read("xyz")).reduce((a, b) => cons(a, b)))

  // Benchmark transactions containing nested reads.
  val depths: Gen[Int] = Gen.range("depth")(1, 10, 1)
  val nested: Gen[Transaction] = depths.map(size => (0 until size).foldLeft(read("xyz"))((a, b) => read(a)))

  performance of "Database" in {
    measure method "execute" in {
      using(sequential) in { txn =>
        Await.result(database.execute(txn), Duration.Inf)
      }

      using(nested) in { txn =>
        Await.result(database.execute(txn), Duration.Inf)
      }
    }
  }

}