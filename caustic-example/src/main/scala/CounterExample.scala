import caustic.runtime.Server
import caustic.runtime.service.Connection
import counter.Counter

object CounterExample extends App {

  // Bootstrap an in-memory database, and establish a connection.
  val server = Server()
  val client = Connection(9090)

  // Create a counter service, and perform increments.
  val service = Counter(this.client)
  this.service.increment("x0")
  this.service.increment("x1")

}
