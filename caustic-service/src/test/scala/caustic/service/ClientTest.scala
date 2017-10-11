package caustic.service

import caustic.runtime.Server
import caustic.runtime.local.LocalDatabase
import caustic.runtime.thrift

import org.junit.runner.RunWith
import org.scalatest.{FunSuite, Matchers}
import org.scalatest.junit.JUnitRunner

import scala.util.Success

@RunWith(classOf[JUnitRunner])
class ClientTest extends FunSuite with Matchers {

  test("Execute works on in-memory server.") {
    // Bootstrap an in-memory database server.
    val server = Server(LocalDatabase(), 9000)

    // Connect and execute transactions.
    val client = Client("localhost", 9000)
    client.execute(cons(write("x", 3), read("x"))) shouldBe Success(thrift.Literal.real(3))

    // Cleanup client and server.
    client.close()
    server.close()
  }

}
