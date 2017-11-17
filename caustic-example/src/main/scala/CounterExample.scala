import caustic.example.counter.Counter
import caustic.runtime.Server
import caustic.runtime.service.Connection

object CounterExample {

  def main(args: Array[String]): Unit = {
    // Bootstrap an in-memory database, and establish a connection.
    val server = Server()
    val client = Connection(9090)

    // Create a counter service, and perform increments.
    val service = Counter(client)
    print(service.increment("x0"))
    print(service.increment("x0"))
    print(service.increment("x1"))

    // Shut down the server.
    client.close()
  }

}
