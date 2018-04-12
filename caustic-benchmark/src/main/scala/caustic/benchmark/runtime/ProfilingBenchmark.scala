package caustic.benchmark.runtime

import caustic.runtime._

/**
 * Benchmarks the CPU and memory footprint of the runtime. To run this benchmark, install JProfiler
 * and acquire a license key from EJ Technologies. Then, install the JProfiler IntelliJ plugin.
 * Optionally enable allocation monitoring for additional visibility into the system.
 */
object ProfilingBenchmark extends App {

  // Construct a program and execute it on a runtime.
  val runtime = Runtime(Volume.Memory())
  val program = Seq.tabulate(4096)(i => write(s"x$i", i)).reduce(cons)
  this.runtime.execute(this.program)

}
