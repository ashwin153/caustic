package caustic.benchmark.runtime

import caustic.runtime._

import java.util.concurrent.atomic.AtomicInteger
import scala.languageFeature.postfixOps
import scala.util.Random

/**
 * Benchmarks the throughput of the runtime under varying workloads. Adjusting the size of the key
 * space and the relative proportion of keys that are read and written in each transaction
 * implicitly changes the contention probability, the likelihood that any two concurrent
 * transactions conflict.
 */
object ThroughputBenchmark extends App {

  val keys     = 10000      // Total number of keys.
  val reads    = 0.10       // Percentage of keys read in each program.
  val writes   = 0.05       // Percentage of keys written in each program.
  val attempts = 1000       // Number of attempts per thread.

  val threads  = java.lang.Runtime.getRuntime.availableProcessors()
  val runtime  = Runtime(Volume.Memory())

  println {
    // Construct the specified number of threads and concurrent generate and execute programs.
    val success = new AtomicInteger(0)
    val threads = Seq.fill(this.threads)(new Thread {
      override def run(): Unit = {
        val results = Seq.fill(attempts)(gen).map(runtime.execute)
        success.addAndGet(results.count(_.isSuccess))
      }
    })

    // Return the total throughput of successfully executed programs.
    val current = System.nanoTime()
    threads.foreach(_.start())
    threads.foreach(_.join())
    val elapsed = System.nanoTime() - current
    1E9 * success.get() / elapsed
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
