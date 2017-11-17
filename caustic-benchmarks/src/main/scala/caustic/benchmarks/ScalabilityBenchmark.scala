package caustic.benchmarks

import caustic.benchmarks.util.Measure
import caustic.runtime._
import caustic.runtime.sql.SQLDatabase

import scala.collection.immutable.Iterable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Random, Success}

object ScalabilityBenchmark {

  def main(args: Array[String]): Unit = {
    val keys     = 10000                   // Size of key space.
    val reads    = 100                     // Number of reads per transaction.
    val writes   = 0                       // Number of writes per transaction.
    val threads  = 2                       // Number of threads executing transactions.
    val times    = 1000                    // Number of attempts per thread.
    val database = SQLDatabase()           // Underlying database.

    println(Measure.throughput {
      // Count the number of successful transactions performed by each thread.
      val successful = Future.sequence(Seq.fill(threads) {
        thread(database, gen(keys, reads, writes), times)
      })

      // Aggregate the total number of successful transactions across all threads.
      Await.result(successful, Duration.Inf).sum
    })
  }

  /**
   * Constructs a thread that attempts to execute a randomly generated transaction the specified
   * number of times on the provided database. Returns the number of successfully performed
   * transactions.
   *
   * @param database Underlying database.
   * @param gen Randomly generated transaction.
   * @param times Number of times.
   * @return Number of successful transactions.
   */
  def thread(database: Database, gen: => Transaction, times: Int): Future[Int] = {
    Future.foldLeft(Iterable.fill(times) {
      database.execute(gen) transform {
        case Success(_) => Success(1)
        case Failure(_) => Success(0)
      }
    })(0)(_ + _)
  }

  /**
   * Generates a randomized transaction that performs the specified number of reads and writes on
   * a key space of the specified size.
   *
   * @param keys Size of key-space.
   * @param reads Number of reads.
   * @param writes Number of writes.
   * @return Randomly generated transaction.
   */
  def gen(keys: Int, reads: Int, writes: Int): Transaction =
    (random(keys, reads).map(read) ++ random(keys, writes).map(write(_, real(1)))).reduce(cons)

  /**
   * Returns a sequence of l integers drawn uniformly at random from [0, n).
   *
   * @param n Population size.
   * @param l Sample size.
   * @return Uniformly random integers.
   */
  def random(n: Int, l: Int): Seq[Transaction] =
    Random.shuffle(Seq.range(0, n)).take(l).map(x => text(x.toString))

}
