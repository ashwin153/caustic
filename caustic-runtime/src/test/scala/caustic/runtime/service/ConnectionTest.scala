package caustic.runtime.service

import caustic.runtime.Server
import caustic.runtime.service._
import caustic.runtime.thrift

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class ConnectionTest extends FunSuite with Matchers {

  test("Execute works on in-memory server.") {
    // Bootstrap an in-memory database server.
    val server = Server()

    // Connect and execute transactions.
    val client = Connection("localhost", 9000)
    client.execute(write("x", 3)) shouldBe Success(thrift.Literal.real(3))

    // Cleanup client and server.
    client.close()
    server.close()
  }

}
