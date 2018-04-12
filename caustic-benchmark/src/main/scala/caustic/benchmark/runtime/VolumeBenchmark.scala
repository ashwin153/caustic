package caustic.benchmark.runtime

import beaker.client._
import caustic.runtime._

import org.scalameter.Bench
import org.scalameter.api.Gen

/**
 * Benchmarks the get and cas performance of the underlying in-memory volume used in other
 * benchmarks. Results show that gets succeed in approximately 6 microseconds even for large
 * transactions. However, cas is several orders of magnitude slower with a cost linear in the size
 * of the transaction. Cas costs around 1 millisecond for small operations and as much as 5
 * milliseconds for larger transactions.
 */
object VolumeBenchmark extends Bench.ForkedTime {

  // Construct an in-memory volume.
  val volume: Volume = Volume.Memory()

  // Benchmark get.
  val get: Gen[Set[String]] = Gen
    .exponential("size")(2, 1 << 12, 2)
    .map(size => Seq.tabulate(size)(i => s"x$i").toSet)

  // Benchmark cas.
  val cas: Gen[(Map[Key, Version], Map[Key, Value])] = get
    .map(keys => (keys.map(_ -> 0L).toMap, keys.map(_ -> "asdf").toMap))

  performance of "Volume.Memory" in {
    measure method "Get" in {
      using(this.get) curve "Get Latency" in { this.volume.get }
    }

    measure method "Cas" in {
      using(this.cas) curve "Cas Latency" in { case (d, c) => this.volume.cas(d, c) }
    }
  }

}
