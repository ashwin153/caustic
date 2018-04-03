package caustic.example.counter

import caustic.runtime.{Volume, Runtime}

object CounterExample extends App {

  // Bootstrap an in-memory database, and establish a connection.
  val runtime = Runtime(Volume.Memory())

  // Create a counter service, and perform increments.
  val service = Counter(this.runtime)
  print(this.service.increment("x0"))
  print(this.service.increment("x0"))
  print(this.service.increment("x1"))

}
