# Runtime
The ```caustic-runtime``` executes transactions on arbitrary key-value stores.

## Getting Started
A ```Server``` may be started and run using [Docker][1]. By default, the ```Server``` will serve an 
in-memory database over port ```9090```. Refer to the [reference configuration][2] for
information about the various configuration parameters and their default values. This configuration
may be optionally overriden by providing a configuration file or setting properties at runtime.

```
docker run -d -p 9090:9090 ashwin153/caustic \             # Serve as daemon on part 9090.
  ./pants run caustic-runtime/src/main/scala:server \      # Run the server binary.
  /path/to/application.conf -- -Dcaustic.server.port=9090  # Optionally override configuration.
```

A ```Server``` may also be run programmatically.

```scala
import caustic.runtime.Server

// Serves an in-memory database over port 9090.
val server = Server()

// Terminates the server.
server.close()
```

A ```Connection``` to this ```Server``` may then be established and used to execute transactions.

```scala
import caustic.runtime.service._

// Establishes a connection to localhost:9090.
val client = Connection(9090)

// Executes a transaction on the remote server.
client.execute(write("x", 3))

// Terminates the connection.
client.close()
```

An exhaustive list of the various supported transactional operations can be found [here][3].

## Service Discovery
A ```Server``` may also be run as a member of a dynamically discoverable ```Cluster``` of instances,
by setting the ```caustic.server.discoverable``` configuration parameter. Service discovery is
managed by a ```Registry``` that notifies clients whenever a ```Server``` comes online or goes 
offline. A ```Registry``` stores its state within a configurable [ZooKeeper][4] ensemble. Clients 
may then connect to this ```Registry``` and use it to execute transactions on registered 
```Server``` instances.

```scala
import caustic.runtime.service._

// Establishes a connection to ZooKeeper.
val registry = Registry()

// Establishes connections to all registered servers.
val client = Cluster(registry)

// Executes a transaction on a random, registered server.
client.execute(write("x", 3))

// Terminates the connections.
client.close()
registry.close()
```

[1]: https://hub.docker.com/r/ashwin153/caustic/
[2]: https://github.com/ashwin153/caustic/blob/master/caustic-runtime/src/main/resources/reference.conf
[3]: https://github.com/ashwin153/caustic/wiki/Runtime#transaction
[4]: https://zookeeper.apache.org/
