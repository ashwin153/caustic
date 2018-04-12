package caustic.benchmark.runtime

import caustic.runtime._

object ProfileBenchmark extends App {

  // Construct a program and execute it on a runtime.
  val runtime = Runtime(Volume.Memory())
  val program = Seq.tabulate(256)(i => read(s"x$i")).reduce(cons)
  this.runtime.execute(this.program)

}
