# Runtime
The ```caustic-runtime``` is responsible for executing transactions on arbitrary key-value stores.

## Server
A ```Server``` may be run using Docker. Configuration overrides may be optionally specified by
passing the path to a configuration file to the binary or by manually providing command line
overrides.

```
docker run -d  -p 9090:9090 ashwin153/caustic \            # Serve as a daemon on port 9090.
  ./pants run caustic-service/src/main/scala:server \      # Run the server binary.
  /path/to/application.conf -- -Dcaustic.server.port=9090  # Optionally override configuration.
```

## Client
A ```Client``` may either be a ```Connection``` or a ```Client```. A ```Connection``` is a direct
connection to a server instance, and is the only way to connect to a ```standalone``` instance. A
 ```Cluster``` is a connection to all the instances in a ```Registry```, and is the recommended way
to connect to a discoverable instance.

```scala
import caustic.runtime.service._
val client = Connection("localhost", 9090)
val registry = Registry("/path/to/application.conf")
val client = Cluster(registry)
```

Once a ```Client``` is constructed, it can be used to execute transactions.
  
```scala
client.execute(write("x", 3))
```
  
Be sure to clean up when you're done!
  
```scala
client.close()
registry.close()
```
  
[1]: https://github.com/ashwin153/caustic/blob/master/caustic-runtime/src/main/resources/reference.conf