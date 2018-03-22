package caustic.example.inherit

import caustic.runtime.{Runtime, Database}

object EmployeeExample extends App {

  // Bootstrap an in-memory database, and establish a connection.
  val runtime = Runtime(Database.Local())

  // Create a counter service, and perform increments.
  val service = Payroll(this.runtime)
  print(this.service.salary(Employee(100)))
  print(this.service.salary(Manager(1000, 100)))

}
