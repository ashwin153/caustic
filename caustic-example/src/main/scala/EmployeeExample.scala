import caustic.example.inherit._
import caustic.runtime.Server
import caustic.runtime.service.Connection

object EmployeeExample {

  def main(args: Array[String]): Unit = {
    // Bootstrap an in-memory database, and establish a connection.
    val server = Server()
    val client = Connection(9090)

    // Create a counter service, and perform increments.
    val service = Payroll(client)
    print(service.salary(Employee(100)))
    print(service.salary(Manager(1, 10)))

    // Shut down the server.
    client.close()
    server.close()
  }

}
