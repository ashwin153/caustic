# Schema
[![Build Status](https://travis-ci.org/ashwin153/schema.svg?branch=master)](https://travis-ci.org/ashwin153/schema)
[![Maven Central](https://img.shields.io/maven-central/v/com.madavan/schema-runtime_2.12.svg)]()

Schema is a Scala DSL for expressing and [optimistically executing][1] database transactions. It is intended to serve as a replacement for SQL, which has reigned as the language of choice for codifying database interactions for the past 43 years. As a motivating example, let's write the same transaction in MySQL and Schema. Both transactions atomically increment a counter ```x```; however, unlike MySQL, Schema does not require an explicit table definition and runs on *any* [transactional key-value store][2] (including MySQL).

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
Schema { implicit ctx =>
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
jar_library(name='schema', jars=[
    jar(org='com.madavan', name='schema-runtime_2.12', rev='1.0.3'),
    jar(org='com.madavan', name='schema-postgresql_2.12', rev='1.0.3'),
    jar(org='com.madavan', name='schema-mysql_2.12', rev='1.0.3'),
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
  "com.madavan" %% "schema-runtime" % "1.0.3",
  "com.madavan" %% "schema-mysql" % "1.0.3",
  "com.madavan" %% "schema-postgresql" % "1.0.3"
)
```

## Maven
```xml
<dependency>
  <groupId>com.madavan</groupId>
  <artifactId>schema-runtime_2.12</artifactId>
  <version>1.0.3</version>
</dependency>

<dependency>
  <groupId>com.madavan</groupId>
  <artifactId>schema-mysql_2.12</artifactId>
  <version>1.0.3</version>
</dependency>

<dependency>
  <groupId>com.madavan</groupId>
  <artifactId>schema-postgresql_2.12</artifactId>
  <version>1.0.3</version>
</dependency>
```

# Documentation
Refer to the [User Guide][6] to learn about how to use the system, and the [wiki][7] for more information.

[1]: https://en.wikipedia.org/wiki/Optimistic_concurrency_control
[2]: https://en.wikipedia.org/wiki/Key-value_database
[3]: https://oss.sonatype.org/index.html#nexus-search;quick~com.madavan
[4]: https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.madavan%22
[5]: https://travis-ci.org/ashwin153/schema
[6]: https://github.com/ashwin153/schema/wiki/User-Guide
[7]: https://github.com/ashwin153/schema/wiki/Home
