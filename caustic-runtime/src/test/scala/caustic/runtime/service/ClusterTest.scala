package caustic.runtime.service

import caustic.runtime.Server
import caustic.runtime.service._
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
    this.curator = CuratorFrameworkFactory.builder()
      .connectString(this.zookeeper.getConnectString)
      .retryPolicy(new ExponentialBackoffRetry(1000, 3))
      .build()

    // Connect to ZooKeeper.
    this.curator.start()
    this.curator.blockUntilConnected()
  }

  override def afterAll(): Unit = {
    this.curator.close()
    this.zookeeper.close()
  }

  test("Execute works on in-memory server.") {
    // Bootstrap a server and register it.
    val registry = Registry(this.curator)
    registry.register(Address("localhost", 9090))
    val server = Server()

    // Execute a transaction.
    val cluster = Cluster(registry)
    eventually(cluster.clients shouldBe 'nonEmpty)
    cluster.execute(write("x", 3)) shouldBe Success(thrift.Literal.real(3))

    // Close the cluster.
    cluster.close()
  }

}