package caustic.example.inherit

import caustic.runtime.Server
import caustic.runtime.service.Connection

object EmployeeExample extends App {

  // Bootstrap an in-memory database, and establish a connection.
  val server = Server()
  val client = Connection(9090)

  // Create a counter service, and perform increments.
  val service = Payroll(this.client)
  print(this.service.salary(Employee(100)))
  print(this.service.salary(Manager(1000, 100)))

  // Shut down the server.
  this.client.close()
  this.server.close()

}
