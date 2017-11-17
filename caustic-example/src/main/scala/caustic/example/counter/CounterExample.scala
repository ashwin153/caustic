package caustic.example.counter

import caustic.runtime.Server
import caustic.runtime.service.Connection

object CounterExample extends App {

  // Bootstrap an in-memory database, and establish a connection.
  val server = Server()
  val client = Connection(9090)

  // Create a counter service, and perform increments.
  val service = Counter(this.client)
  print(this.service.increment("x0"))
  print(this.service.increment("x0"))
  print(this.service.increment("x1"))

  // Shut down the client and server.
  this.client.close()
  this.server.close()

}
