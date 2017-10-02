# Runtime
The ```caustic-runtime``` is responsible for executing transactions on arbitrary key-value stores.

## Setup
### Standalone
A ```Server``` may be run as a standalone instance. For example, we can bootstrap an in-memory ```Server``` and execute transactions on a corresponding ```Connection``` from directly within REPL. Enter a REPL by running ```./pants repl caustic-runtime/src/main/scala``` from the project root. First, import the global execution context, the in-memory runtime, and the external service interface.

```scala
import scala.concurrent.ExecutionContext.Implicits.global
import caustic.runtime.memory._
import caustic.runtime.service._
```

Then, bootstrap an in-memory database ```Server``` and start it as a background process.

```scala
val server = Server(LocalDatabase.empty, 9000)
server.start()
```

Finally, open a ```Connection``` to the ```Server``` and use it to execute transactions.

```scala
val client = Connection("localhost", 9000)
client.execute(cons(write("x", 3), read("x")))
```

It's good practice to close the clients and stop servers when they are no longer needed.

```scala
client.close()
server.stop()
```

## Cluster
A ```Server``` may also be run as a member of a cluster of instances. Caustic relies on [ZooKeeper][3] to perform automatic server registration and discovery. The following script will download, install, and start ZooKeeper on macOS. A more in depth tutorial can be found [here][4].

```sh
brew install zookeeper
brew services start zookeeper
```

Once ZooKeeper is up and running, we'll need to create ```Registry``` to store the locations of the various active server instances. To construct a ```Registry```, we'll need to supply (a) a [Curator][5] connection to ZooKeeper and (b) a Zookeeper path to the registry contents.

```scala
import org.apache.curator.framework.CuratorFrameworkFactory

val curator = CuratorFrameworkFactory.newClient(
  "localhost:2181,localhost:3001", 
  new ExponentialBackoffRetry(1000, 3)
)

val registry = Registry(curator, "/services/caustic")
```

Then, we can use this ```Registry``` to bootstrap a database ```Server``` that automatically announces itself when it comes online and removes itself when it goes offline.

```scala
val server = Server(registry, LocalDatabase.empty, 9000)
server.start()
```

Finally, we can connect a ```Cluster``` to the ```Registry``` and use it to execute transactions on a randomized ```Server``` instance. Randomization (in)effectively distributes execute calls across the various instances in the registry.

```scala
val client = Cluster(registry)
client.execute(cons(write("x", 3), read("x")))
```

Always be sure the clean up after you're done!

```
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
[3]: https://zookeeper.apache.org/
[4]: https://blog.kompany.org/2013/02/23/setting-up-apache-zookeeper-on-os-x-in-five-minutes-or-less/
[5]: http://curator.apache.org/curator-framework/
