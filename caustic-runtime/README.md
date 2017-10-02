# Runtime
The ```caustic-runtime``` is responsible for executing transactions on arbitrary key-value stores. In order to use a ```Database```, simply construct and serve a Thrift server in the following manner.

## Standalone
```scala
import scala.concurrent.ExecutionContext.Implicits.global
import caustic.runtime.memory._
import caustic.runtime.service._

// Bootstrap an in-memory server.
val server = Server(LocalDatabase.empty, 9000)
server.start()

// Connect a client and execute transactions.
val client = Connection("localhost", 9000)
client.execute(cons(write("x", 3), read("x")))

// Cleanup the client and server.
client.close()
server.stop()
```

## Cluster
Caustic relies on ZooKeeper to perform automatic service discovery of server instances in a cluster. The following script installs ZooKeeper and verifies that it is running correctly. A more detailed guide can be found [here](https://blog.kompany.org/2013/02/23/setting-up-apache-zookeeper-on-os-x-in-five-minutes-or-less/).

```sh
# Install and Start ZooKeeper.
brew install zookeeper
brew services start zookeeper

# Login to ZooKeeper.
# Commands: http://zookeeper.apache.org/doc/r3.4.5/zookeeperAdmin.html#sc_zkCommands
telnet localhost 2181
stat
```

Once ZooKeeper is up and running, we'll need to create a ```Registry``` which each ```Server``` will announce themself in so that each ```Client``` can automatically discover their network location.

```scala
import org.apache.curator.framework.CuratorFrameworkFactory

// Connect to ZooKeeper.
val curator = CuratorFrameworkFactory.newClient(
  "localhost:2181,localhost:3001", 
  new ExponentialBackoffRetry(1000, 3)
)

// Construct a Registry.
val registry = Registry(curator, "/services/caustic")
```

Finally, we'll setup a ```Server``` that automatically announces itself in the registry and a ```Cluster``` which executes transactions on a randomized instance in the registry. Randomization enables the ```Cluster``` to distribute load (somewhat inequitably) across the various instances in the registry.

```scala
// Bootstrap a server instance.
val server = Server(registry, LocalDatabase.empty, 9000)
server.start()

// Connect a cluster client and execute transactions.
val client = Cluster(registry)
client.execute(cons(write("x", 3), read("x")))

// Cleanup the cluster and server.
client.close()
server.stop()
```

## Representation
A ```Transaction``` is represented by an [abstract-syntax tree][1]. A ```Literal``` corresponds to a "leaf" of this tree and an ```Expression``` corresponds to a "node". The runtime respects three different types of literals ```Real```, ```Text```, and ```Flag``` (corresponding to ```String```, ```Double```, and ```Boolean``` respectively) and thirty different kinds of expressions. With just these types and operations, the runtime is able to emulate a modern programming language complete with conditional branching, loops, and local variables.

For example, the transaction ```write(text("x"), add(read(text("x")), real(1)))``` increments the counter ```x```. While this language is fully featured, it is extremely verbose and very unintuitive. In the next section, we'll implement a more convenient syntax for expressing transactions than these primitive syntax trees.

## Interoperability
The runtime supports any asynchronous, transactional key-value store that satisfies the following interface. A ```Key``` is a unique ```String``` and a ```Revision``` is a tuple containing a version number and a literal value, which the runtime uses to implement [Multiversion Concurrency Control][2].

```scala
/**
 * An asynchronous, transactional key-value store.
 */
trait Database {

  /**
   * Asynchronously returns the latest revisions of the specified keys.
   *
   * @param keys Lookup keys.
   * @param ec Implicit execution context.
   * @return Latest revisions of the specified keys.
   */
  def get(keys: Set[Key])(
    implicit ec: ExecutionContext
  ): Future[Map[Key, Revision]]

  /**
   * Asynchronously applies the specified changes if and only if the revisions of the 
   * specified dependent keys remain constant and returns an exception on conflict. The 
   * consistency, durability, and availability of the system is determined by the 
   * implementation of this conditional put operator.
   *
   * @param depends Dependencies.
   * @param changes Updates.
   * @param ec Implicit execution context.
   * @return Future that completes when successful, or an exception otherwise.
   */
  def cput(depends: Map[Key, Version], changes: Map[Key, Revision])(
    implicit ec: ExecutionContext
  ): Future[Unit]
```

## Execution
Transactions are executed through repeated partial evaluation, according to the following procedure.

1. __Fetch__: Call ```get``` on all keys that are read and written by the transaction, and add the returned revisions to a local snapshot. In order to guarantee snapshot isolation, ```get``` is only called when a key is read or written *for the first time*. By batching reads and writes and avoiding duplicate fetches, the runtime is guaranteed to perform a minimal number of roundtrips to and from the database. 
2. __Evaluate__: Recursively replace all expressions that have literal operands with their corresponding literal result. For example, ```add(real(1), sub(real(0), real(2)))``` returns ```real(-1)```. The result of all ```write```expressions is saved to a local buffer and all ```read``` expressions return the latest value of the key in either the local buffer or snapshot.  
3. __Repeat__: Re-evaluate the transaction until it reduces to a single literal value. Because all expressions with literal operands return a literal value, all transactions eventually reduce to a literal.

[1]: https://en.wikipedia.org/wiki/Abstract_syntax_tree
[2]: https://en.wikipedia.org/wiki/Multiversion_concurrency_control
