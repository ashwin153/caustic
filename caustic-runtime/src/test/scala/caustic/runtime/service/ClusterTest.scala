package caustic.runtime.service

import caustic.runtime.memory.LocalDatabase
import caustic.runtime.thrift

import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class ClusterTest extends FunSuite with Matchers {

  test("Execute works on in-memory server.") {
    // Setup the ZooKeeper registry.
    val zkretry = new ExponentialBackoffRetry(1000, 3)
    val curator = CuratorFrameworkFactory.newClient("localhost:2181", zkretry)
    val registry = Registry(curator, "/services/caustic")
    curator.start()

    // Boostrap an in-memory database server.
    val server = Server(LocalDatabase.empty, 9000, registry)
    server.start()

    // Connect and execute transactions.
    val client = Cluster(registry)
    client.execute(cons(write("x", 3), read("x"))) shouldBe Success(thrift.Literal.real(3))

    // Cleanup client, server, and curator.
    client.close()
    server.stop()
    curator.close()
  }

}
