# Caustic
[![Build Status](https://travis-ci.org/ashwin153/caustic.svg?branch=master)](https://travis-ci.org/ashwin153/caustic)
[![Maven Central](https://img.shields.io/maven-central/v/com.madavan/caustic-runtime_2.12.svg)]()

Databases are either easy to use or easy to scale. For example, relational databases were popularized in large part because of the programmability of SQL. However, this programmability comes at an incredible expense. Relational databases are notoriously difficult to support at scale, and so developers have increasingly turned toward more specialized NoSQL systems that scale well be shedding functionality.

Developers are not only forced to choose between productivity and performance, but also stark differences between query languages makes their choice of database effectively permanent. Even databases that claim to support the same SQL standard only implement incompatible subsets of its functionality. The lack of a truly uniform interface tightly couples databases and the procedures that are executed against it.

Caustic is a language for expressing and executing transactions on arbitrary key-value stores that is both straightforward to use and simple to integrate. In this article, we’ll discuss the implementation and implications of the transaction runtime and syntax. As a motivating example, consider the following distributed counter implementation in MySQL and Caustic. Both transactions have identical functionality; however, the Caustic transaction is much simpler and will run on *any* [transactional key-value store][2] (including MySQL!).

```sql
CREATE TABLE `counters` (
  `key` varchar(250) NOT NULL,
  `value` BIGINT,
  PRIMARY KEY (`key`)
)

START TRANSACTION;
INSERT INTO `counters` (`key`, `value`) VALUES ("x", 1) 
ON DUPLICATE KEY UPDATE `value` = `value` + 1
COMMIT;
```

```
module caustic.example

record Total {
  value: Integer
}

service Counter {
          
  def increment(x: Total): Integer = {
    if (x.exists) {
      x.value += 1
    } else {
      x.value = 1
    }
  }

} 
```

# Requirements
- Scala 2.12
- Java 1.8
- Python 2.7
- PostgreSQL 9.5
- MySQL 5.0

# Build
Artifacts are published to the [Sonatype OSS Repository Hosting Service][3] and synced to [Maven Central][4]. Snapshots of the ```master``` branch are built using [Travis CI][5].

## Pants
```python
jar_library(name='caustic', jars=[
    jar(org='com.madavan', name='caustic-runtime_2.12', rev='1.0.0'),
    jar(org='com.madavan', name='caustic-postgres_2.12', rev='1.0.0'),
    jar(org='com.madavan', name='caustic-mysql_2.12', rev='1.0.0'),
])
```

## SBT
```scala
scalaVersion := "2.12.1"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  "com.madavan" %% "caustic-runtime" % "1.0.0",
  "com.madavan" %% "caustic-mysql" % "1.0.0",
  "com.madavan" %% "caustic-postgres" % "1.0.0"
)
```

## Maven
```xml
<dependency>
  <groupId>com.madavan</groupId>
  <artifactId>caustic-runtime_2.12</artifactId>
  <version>1.0.0</version>
</dependency>

<dependency>
  <groupId>com.madavan</groupId>
  <artifactId>caustic-mysql_2.12</artifactId>
  <version>1.0.0</version>
</dependency>

<dependency>
  <groupId>com.madavan</groupId>
  <artifactId>caustic-postgres_2.12</artifactId>
  <version>1.0.0</version>
</dependency>
```

# Documentation
Refer to the [User Guide][6] to learn about how to use the system, and the [Wiki][7] for more information.

[1]: https://en.wikipedia.org/wiki/Optimistic_concurrency_control
[2]: https://en.wikipedia.org/wiki/Key-value_database
[3]: https://oss.sonatype.org/index.html#nexus-search;quick~com.madavan
[4]: https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.madavan%22
[5]: https://travis-ci.org/ashwin153/caustic
[6]: https://github.com/ashwin153/caustic/wiki/User-Guide
[7]: https://github.com/ashwin153/caustic/wiki/Home
