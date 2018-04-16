package caustic.benchmark

import caustic.benchmark.runtime._

/**
 * A benchmarking suite.
 */
object Suite extends App {

  // Execute the specified benchmark.
  this.args(0) match {
    case "latency" => LatencyBenchmark.main(args.drop(1))
    case "throughput" => ThroughputBenchmark.main(args.drop(1))
    case "volume" => VolumeBenchmark.main(args.drop(1))
    case "profile" => ProfileBenchmark.main(args.drop(1))
  }

}
