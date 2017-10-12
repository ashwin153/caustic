# Caustic
[![Build Status](https://travis-ci.org/ashwin153/caustic.svg?branch=master)][3]
[![Maven Central](https://img.shields.io/maven-central/v/com.madavan/caustic-runtime_2.12.svg)][2]

Databases are either easy to use or easy to scale. For example, relational databases were 
popularized in large part because of the programmability of SQL. However, this programmability comes 
at an incredible expense. Relational databases are notoriously difficult to support at scale, and so 
developers have increasingly turned toward more specialized NoSQL systems that scale well be 
shedding functionality.

Developers are not only forced to choose between productivity and performance, but also stark 
differences between query languages makes their choice of database effectively permanent. Even 
databases that claim to support the same SQL standard only implement incompatible subsets of its 
functionality. The lack of a truly uniform interface tightly couples databases and the procedures 
that are executed against them.

Caustic is a language for expressing and executing transactions on arbitrary key-value stores that 
is both straightforward to use and simple to integrate. 

## Requirements
- Java 1.8
- MySQL 5.0+
- PostgreSQL 9.5+
- Python 2.7
- Scala 2.12
- ZooKeeper 3.4.10

## Artifacts
Artifacts are published to the [Sonatype OSS Repository Hosting Service][1] and synced to 
[Maven Central][2]. Snapshots of the ```master``` branch are built using [Travis CI][3] and images
are available on [Docker][6]. The Maven coordinates of core build artifacts are as follows.

```
<!-- Client Library -->
<dependency>
  <groupId>com.madavan</groupId>
  <artifactId>caustic-client_2.12</artifactId>
  <version>1.3.1</version>
</dependency>

<!-- Runtime Library -->
<dependency>
  <groupId>com.madavan</groupId>
  <artifactId>caustic-runtime_2.12</artifactId>
  <version>1.3.1</version>
</dependency>
```

## Documentation
Refer to the [User Guide][4] to learn about how to use the system, and the [Wiki][5] for more 
information about the implementation.

[1]: https://oss.sonatype.org/index.html#nexus-search;quick~com.madavan
[2]: https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.madavan%22
[3]: https://travis-ci.org/ashwin153/caustic
[4]: https://github.com/ashwin153/caustic/wiki/User-Guide
[5]: https://github.com/ashwin153/caustic/wiki/Home
[6]: https://hub.docker.com/r/ashwin153/caustic/
