package caustic.service

import caustic.runtime.memory.LocalDatabase
import caustic.runtime.thrift
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.test.TestingServer
import org.junit.runner.RunWith
import org.scalatest.concurrent.Eventually
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class ClusterTest extends FunSuite with Matchers with BeforeAndAfterAll with Eventually {

  var zookeeper: TestingServer = _
  var curator: CuratorFramework = _

  override def beforeAll(): Unit = {
    // Setup ZooKeeper.
    this.zookeeper = new TestingServer(true)
    this.curator = CuratorFrameworkFactory.newClient(
      this.zookeeper.getConnectString,
      new ExponentialBackoffRetry(1000, 3)
    )

    // Connect to ZooKeeper.
    this.curator.start()
    this.curator.blockUntilConnected()
  }

  override def afterAll(): Unit = {
    this.curator.close()
    this.zookeeper.close()
  }

  test("Execute works on in-memory server.") {
    // Bootstrap an in-memory database server.
    val registry = Registry(this.curator, "/services/caustic")
    val server = Server(LocalDatabase.empty, 9000, registry)
    server.serve()

    // Connect and execute transactions.
    val client = Cluster(registry)
    eventually(client.connections shouldBe 'nonEmpty)
    client.execute(cons(write("x", 3), read("x"))) shouldBe Success(thrift.Literal.real(3))

    // Cleanup client and server.
    client.close()
    server.close()
  }

}
