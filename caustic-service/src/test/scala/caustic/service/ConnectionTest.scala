package caustic.service

import caustic.runtime.memory.LocalDatabase
import caustic.runtime.thrift

import org.junit.runner.RunWith
import org.scalatest.{FunSuite, Matchers}
import org.scalatest.junit.JUnitRunner
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class ConnectionTest extends FunSuite with Matchers {

  test("Execute works on in-memory server.") {
    // Boostrap an in-memory database server.
    val server = Server(LocalDatabase.empty, 9000)
    server.serve()

    // Connect and execute transactions.
    val client = Connection("localhost", 9000)
    client.execute(cons(write("x", 3), read("x"))) shouldBe Success(thrift.Literal.real(3))

    // Cleanup client and server.
    client.close()
    server.close()
  }

}
