# Service
The ```caustic-service``` package provides a convenient interface for bootstrapping and executing
 transactions on the ```caustic-runtime```.

## Server
A ```Server``` may be run as a ```standalone``` or a ```discoverable``` instance. A 
```discoverable``` instance automatically registers its network address in a ```Registry``` so that 
the various clients can dynamically discover its location. A ```standalone``` instance operates in
isolation and is only accessible by clients with prior knowledge of their network address. An 
instance of each type may be bootstrapped in the following manner.

```
./pants run caustic-service/src/main/scala:standalone
./pants run caustic-service/src/main/scala:discoverable
```

Configuration overrides may be passed directly on the command line or through an optionally provided 
path to a configuration file that overrides the [default server configuration][1]. Because a 
```Registry``` uses ZooKeeper as an underlying source of consistency and durability, 
[further configuration parameters][2] are required for ```discoverable``` instances.

```
./pants run caustic-service/src/main/scala:standalone /path/to/application.conf -- -Dprop=value
```

## Client
A ```Client``` may either be a ```Connection``` or a ```Client```. A ```Connection``` is a direct
connection to a server instance, and is the only way to connect to a ```standalone``` instance. A
```Cluster``` is a connection to all the instances in a ```Registry```, and is the recommended way
to connect to a ```discoverable``` instance.

```scala
import caustic.service.client._
val client = Connection("localhost", 9090)
val registry = Registry("/path/to/application.conf")
val client = Cluster(registry)
```

Once a ```Client``` is constructed, it can be used to execute transactions.

```scala
import caustic.service.client._
client.execute(write("x", 3))
```

Be sure to clean up when you're done!

```scala
client.close()
registry.close()
```

## Discovery
Service discovery is implemented on top of ZooKeeper and Curator. While Curator does already provide
an implementation of [service discovery][3], I found it to be a verbose, unintuitive interface and so
I elected to implement my own version instead. Due to assumptions made by my underlying
dependencies, the implementation of service discovery __requires__ ZooKeeper 3.4.10 and differences
in version numbers may cause undefined behavior. 

[1]: https://github.com/ashwin153/caustic/blob/master/caustic-runtime/src/main/resources/reference.conf
[2]: https://github.com/ashwin153/caustic/tree/master/caustic-service/src/main/resources/reference.conf
[3]: https://github.com/Netflix/curator/wiki/Service-Discovery