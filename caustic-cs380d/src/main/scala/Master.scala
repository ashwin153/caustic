import caustic.beaker._
import caustic.beaker.storage.Local
import caustic.cluster.Address
import caustic.cluster.protocol.{Test, Beaker => External}
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn

object Master extends App {

  type ClientId = Int
  type ServerId = Int

  // https://stackoverflow.com/a/16256935/1447029
  implicit class Regex(sc: StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }

  val global  = Test.Cluster[External.Client](External.Service)
  val current = mutable.Map[(ClientId, Key), Long]()
  val clients = mutable.Map[ClientId, Test.Cluster[External.Client]]()
  val servers = mutable.Map[ServerId, Beaker.Server]()

  // Read the program from StdIn, and block until it is fully loaded.
  val program = Iterator.continually(StdIn.readLine).takeWhile(_ != "EOF").mkString("\n")

  // Sequentially execute the program.
  this.program.split("\n") foreach {
    case r"joinServer (\d+)${serverId}" =>
      // Starts a server and will connect this server to all other servers in the system.
      val address  = Address.local(9090 + serverId.toInt)
      val database = Local.Database()
      val cluster  = Test.Cluster(Internal.Service)
      val server   = Beaker.Server(address, database, cluster)
      this.servers += serverId.toInt -> server
      this.servers.values.foreach(s => cluster.join(s.address))
      this.servers.values.foreach(s => s.cluster.join(address))

      server.serve()
      this.global.join(address)

    case r"killServer (\d+)${serverId}" =>
      // Immediately kills a server. Blocks until the server is stopped.
      this.servers.remove(serverId.toInt) foreach { server =>
        this.servers.values.foreach(_.cluster.leave(server.address))
        server.close()
        this.global.leave(server.address)
      }

    case r"joinClient (\d+)${clientId} (\d+)${serverId}" =>
      // Starts a client and connects the client to the specified server.
      val server = this.servers(serverId.toInt)
      val cluster = this.clients.getOrElseUpdate(clientId.toInt, Test.Cluster(External.Service))
      cluster.join(server.address)

    case r"breakConnection (\d+)${id1} (\d+)${id2}" =>
      // Breaks the connection between a client and a server or between two servers.
      val server = this.servers(id2.toInt)
      this.clients.get(id1.toInt).foreach(_.leave(server.address))
      this.servers.get(id1.toInt) foreach { s => s.cluster.leave(server.address); server.cluster.leave(s.address) }

    case r"createConnection (\d+)${id1} (\d+)${id2}" =>
      // Creates or restores the connection between a client and a server or between two servers.
      val server = this.servers(id2.toInt)
      this.clients.get(id1.toInt).foreach(_.join(server.address))
      this.servers.get(id1.toInt) foreach { s => s.cluster.join(server.address); server.cluster.join(s.address) }

    case r"stabilize" =>
      // Blocks until all values are able to propagate to all connected servers. This should block
      // for a maximum of 5 seconds for a system with 5 servers.
      val keys = this.current.keys.map(_._2).toSet
      this.global.broadcast(_.refresh(keys))

    case r"printStore (\d+)${serverId}" =>
      // Prints out a server’s key-value store.
      val keys = this.current.keys.map(_._2).toSet
      val client = External.Service.connect(this.servers(serverId.toInt).address)
      client.get(keys) foreach { case (k, r) => println(s"$k:${r.value}") }
      External.Service.disconnect(client)

    case r"put (\d+)${clientId} ([0-1]+)${key} ([0-1]+)${value}" =>
      // Associates the given value with the key.
      val rev = Await.result(this.clients(clientId.toInt).random(_.put(key, value)), Duration.Inf)
      this.current += (clientId.toInt, key) -> rev.getOrElse(0L)

    case r"get (\d+)${clientId} ([0-1]+)${key}" =>
      // Tells a client to attempt to get the key associated with the given value. The value or
      // error returned should be printed to standard-out. Blocks until the client communicates with
      // a server and the master script.
      val latest = this.current.getOrElse((clientId.toInt, key), 0L)
      val value  = Await.result(this.clients(clientId.toInt).random(_.get(key)), Duration.Inf)

      println(s"$key:${value match {
        case Some(r) if r.version < latest => "ERR_DEP"
        case Some(r) => this.current += (clientId.toInt, key) -> r.version; r.value
        case _ => "ERR_KEY"
      }}")

  }

  System.exit(0)

}