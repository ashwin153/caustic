package caustic.benchmark.runtime

import caustic.runtime._

import scala.util.Random

/**
 * Benchmarks the throughput of the runtime under varying workloads. Adjusting the size of the key
 * space and the relative proportion of keys that are read and written in each transaction
 * implicitly changes the contention probability, the likelihood that any two concurrent
 * transactions conflict.
 */
object ThroughputBenchmark extends App {

  val keys     = 1000       // Total number of keys.
  val reads    = 0.01       // Percentage of keys read in each program.
  val writes   = 0.00       // Percentage of keys written in each program.
  val attempts = 1000       // Number of attempts per thread.

  val threads  = java.lang.Runtime.getRuntime.availableProcessors()
  val runtime  = Runtime(Volume.Memory())

  println {
    // Construct the specified number of threads and concurrent generate and execute programs.
    val program = Seq.fill(this.threads)(Seq.fill(attempts)(gen))
    val current = System.nanoTime()
    val success = program.par.map(_.map(runtime.execute).count(_.isSuccess)).sum
    val elapsed = System.nanoTime() - current
    1E9 * success / elapsed
  }

  /**
   * Generates a randomized transaction that performs the specified number of reads and writes on
   * a key space of the specified size.
   *
   * @return Randomly generated transaction.
   */
  def gen: Program =
    (random(keys, reads).map(read) ++ random(keys, writes).map(write(_, real(1)))).reduce(cons)

  /**
   * Returns a sequence of l integers drawn uniformly at random from [0, n).
   *
   * @param n Population size.
   * @param l Sample size.
   * @return Uniformly random integers.
   */
  def random(n: Int, l: Double): Seq[Program] =
    Random.shuffle(Seq.range(0, n)).take((n * l).toInt).map(x => text(x.toString))

}
