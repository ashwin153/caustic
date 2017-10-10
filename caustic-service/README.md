# Service
The ```caustic-service``` package provides a convenient interface for executing transactions on the ```caustic-runtime```.

## Terminology
- A ```Server``` executes a ```Transaction``` on a ```Database```.
- A ```Client``` connects to a ```Server```.
- A ```Connection``` is a ```Client``` that connects to a single ```Server```.
- A ```Registry``` stores the network location of each ```Server```.
- A ```Cluster``` is a ```Client``` that connects to each ```Server``` in a ```Registry```.

## Examples
### Standalone Server
A ```Server``` may be run as a standalone instance. In this example, we'll bootstrap an 
in-memory ```Server``` and execute a ```Transaction``` on it using a ```Connection``` from
directly within the Scala REPL. Enter a REPL session by running the following command from the
project root: ```./pants repl caustic-service/src/main/scala```.

1. Bootstrap an in-memory ```Database``` and serve it as a standalone ```Server```.

```scala
import caustic.runtime.memory.LocalDatabase
import caustic.service._

val server = Server(LocalDatabase.empty, 9000)
server.serve()
```

2. Open a ```Connection``` to the ```Server``` and use it to execute a ```Transaction```.

```scala
val client = Connection("localhost", 9000)
client.execute(cons(write("x", 3), read("x")))
```

3. Close ```Client``` and ```Server``` when they are no longer needed.

```scala
client.close()
server.close()
```

## Discoverable Server
A ```Server``` may also be run as a discoverable instance. Service discovery is implemented on top 
of [ZooKeeper][1] (__version 3.4.10__) and [Curator][3]. While Curator itself does already provide 
an implementation of [service discovery][4], I personally found it to be a verbose, unintuitive 
interface and so I chose to implement my own version of service discovery instead. Before starting 
with the example, we'll first need to setup ZooKeeper. The following script will download, install, 
and start ZooKeeper on macOS using Homebrew. A more in-depth tutorial can be found [here][2] for 
other systems and configurations.

```sh
brew install zookeeper
brew services start zookeeper
```

In this example, we'll use this ZooKeeper connection to bootstrap an in-memory, discoverable 
```Server``` and execute a ```Transaction``` on it using a ```Cluster``` from directly within the 
Scala REPL. Enter a REPL session by running the following command from the project root: 
```./pants repl caustic-service/src/main/scala```.

1. Establish a programmatic connection our local ZooKeeper instance using Curator.

```
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry

val curator = CuratorFrameworkFactory.newClient(
  "localhost:2181",                    // Comma delimited list of ZooKeeper host:port.
  new ExponentialBackoffRetry(1000, 3) // Recommended default retry policy.
)

curator.start()
```

2. Import the service discovery package, and create a ```Registry``` in which each ```Server```
   announces their ```Address```. The contents of the ```Registry``` are stored in a user-specified
   directory within ZooKeeper.

```scala
import caustic.service._

val registry = Registry(curator, "/services/caustic")
```

3. Bootstrap an in-memory ```Database``` and serve it as a discoverable ```Server```.

```
import caustic.runtime.memory.LocalDatabase

val server = Server(LocalDatabase.empty, 9000, registry)
server.serve()
```

4. Open a ```Cluster``` client to the ```Registry``` and use it to execute a ```Transaction``` on a
   randomized, registered instance. Randomization (in)effectively distributes requests across the
   various instances in the registry.
   
```scala
val client = Cluster(registry)
client.execute(cons(write("x", 3), read("x")))
```

5. Always be sure the clean up after you're done!

```
client.close()
server.close()
curator.close()
```

[1]: https://zookeeper.apache.org/
[2]: https://blog.kompany.org/2013/02/23/setting-up-apache-zookeeper-on-os-x-in-five-minutes-or-less/
[3]: http://curator.apache.org/curator-framework/
[4]: https://github.com/Netflix/curator/wiki/Service-Discovery
