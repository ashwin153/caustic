package caustic.benchmark.runtime

import caustic.runtime._

import org.scalameter.Bench
import org.scalameter.api._

/**
 * Benchmarks the latency of the runtime. Results show that the runtime scales linearly with
 * transaction size. We show that Caustic adds tens of milliseconds to the runtime of transactions
 * that span thousands of keys.
 */
object LatencyBenchmark extends Bench.ForkedTime {

  // Construct an in-memory runtime.
  val runtime: Runtime = Runtime(Volume.Memory())

  // Benchmark reads.
  val reads: Gen[Program] = Gen
    .exponential("size")(2, 1 << 12, 2)
    .map(size => Seq.tabulate(size)(i => read(s"x$i")).reduce(cons))

  // Benchmark writes.
  val writes: Gen[Program] = Gen
    .exponential("size")(2, 1 << 12, 2)
    .map(size => Seq.tabulate(size)(i => write(s"x$i", i)).reduce(cons))

  // Benchmark writes then reads.
  val readAfterWrites: Gen[Program] = Gen
    .exponential("size")(2, 1 << 12, 2)
    .map(_ / 2)
    .map(size => Seq.tabulate(size)(i => cons(write(s"x$i", i), read(s"x$i"))).reduce(cons))

  performance of "Runtime" in {
    measure method "Runtime.execute" in {
      using(this.reads) curve "Read Latency" in { this.runtime.execute }
      using(this.writes) curve "Write Latency" in { this.runtime.execute }
      using(this.readAfterWrites) curve "Read-After-Write Latency" in { this.runtime.execute }
    }
  }

}
