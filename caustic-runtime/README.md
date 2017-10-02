# Runtime
The ```caustic-runtime``` is responsible for executing transactions on arbitrary key-value stores.

## Standalone
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

It's always good practice to close clients and stop servers when they are no longer needed.

```scala
client.close()
server.stop()
```

## Cluster
A ```Server``` may also be run as a member of a cluster of instances. Caustic depends on [ZooKeeper][1] __version 3.4.10__ to perform automatic server registration and discovery. The following script will download, install, and start ZooKeeper on macOS. A more in depth tutorial can be found [here][2].

```sh
brew install zookeeper
brew services start zookeeper
```

Once ZooKeeper is up and running, we'll need to create ```Registry``` to store the locations of the various active ```Server``` instances. To construct a ```Registry```, we'll need to supply (a) a [Curator][3] connection to ZooKeeper and (b) a Zookeeper path to its contents. While Curator does already provide an implementation of [service discovery][4], I personally found it to be a verbose, unintuitive interface and so I chose to implement my own version instead.

```scala
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry

val curator = CuratorFrameworkFactory.newClient(
  "localhost:2181",                    // Comma delimited list of ZooKeeper host:port.
  new ExponentialBackoffRetry(1000, 3) // Recommended default retry policy.
)

val registry = Registry(curator, "/services/caustic")
curator.start()
```

Then, we can use this ```Registry``` to bootstrap a database ```Server``` that automatically registers itself when it comes online and unregisters itself when it goes offline and start it as a background process.

```scala
val server = Server(LocalDatabase.empty, 9000, registry)
server.start()
```

Finally, we can connect a ```Cluster``` to the ```Registry``` and use it to execute transactions on a randomized, registered ```Server``` instance. Randomization (in)effectively distributes execute calls across the various instances.

```scala
val client = Cluster(registry)
client.execute(cons(write("x", 3), read("x")))
```

Always be sure the clean up after you're done!

```
client.close()
server.stop()
curator.close()
```

[1]: https://zookeeper.apache.org/
[2]: https://blog.kompany.org/2013/02/23/setting-up-apache-zookeeper-on-os-x-in-five-minutes-or-less/
[3]: http://curator.apache.org/curator-framework/
[4]: https://github.com/Netflix/curator/wiki/Service-Discovery
