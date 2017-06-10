# Caustic
[![Build Status](https://travis-ci.org/ashwin153/caustic.svg?branch=master)](https://travis-ci.org/ashwin153/caustic)
[![Maven Central](https://img.shields.io/maven-central/v/com.madavan/caustic-runtime_2.12.svg)]()

Caustic is a Scala DSL for expressing and [optimistically executing][1] database transactions. It is intended to serve as a replacement for SQL, which has reigned as the language of choice for codifying database interactions for the past 43 years. As a motivating example, let's write the same transaction in MySQL and Caustic. Both transactions atomically increment a counter ```x```; however, unlike MySQL, Caustic does not require an explicit table definition and runs on *any* [transactional key-value store][2] (including MySQL).

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

```scala
db.execute { implicit ctx =>
  val counter = Select("x")
  If (!counter.exists) {
    counter.value = 1
  } Else {
    counter.value += 1
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
    jar(org='com.madavan', name='caustic-runtime_2.12', rev='1.0.3'),
    jar(org='com.madavan', name='caustic-postgres_2.12', rev='1.0.3'),
    jar(org='com.madavan', name='caustic-mysql_2.12', rev='1.0.3'),
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
  "com.madavan" %% "caustic-runtime" % "1.0.3",
  "com.madavan" %% "caustic-mysql" % "1.0.3",
  "com.madavan" %% "caustic-postgres" % "1.0.3"
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
