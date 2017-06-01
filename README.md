# Schema
Schema is a library for expressing and executing database transactions. Schema provides a dynamically-typed language to **express** transactions and utilizes [Multiversion Concurrency Control](https://en.wikipedia.org/wiki/Multiversion_concurrency_control) to optimistically and efficiently **execute** transactions on *arbitrary* key-value stores. The following are transactions written in Schema and MySQL to give you a taste of what the library can do.

```scala
Schema { implicit ctx =>
  val counter = Select("x")
  If (!counter.exists) {
    counter.total = 0
  } Else {
    counter.total += 1
  }
}
```

```sql
CREATE TABLE `counters` (
  `key` varchar(250) NOT NULL,
  `total` BIGINT,
  PRIMARY KEY (`key`)
)

START TRANSACTION;
INSERT INTO `counters` (`key`, `total`) VALUES ("x", 1)
ON DUPLICATE KEY UPDATE `value` = `value` + 1
COMMIT;
```

## Overview
- ```schema-runtime/```: Core runtime library
- ```schema-mysql/```: MySQL integration
- ```schema-postgresql/```: PostgreSQL integration
- ```schema-benchmark/```: Performance tests

## Build
Artifacts are synced to Maven Central.

```xml
<dependency>
  <groupId>com.madavan</groupId>
  <artifactId>schema-runtime_2.12</artifactId>
  <version>1.0.3</version>
</dependency>
```

```xml
<dependency>
  <groupId>com.madavan</groupId>
  <artifactId>schema-mysql_2.12</artifactId>
  <version>1.0.3</version>
</dependency>
```

```xml
<dependency>
  <groupId>com.madavan</groupId>
  <artifactId>schema-postgresql_2.12</artifactId>
  <version>1.0.3</version>
</dependency>
```

## Documentation
Refer to the [User Guide](https://github.com/ashwin153/schema/wiki/User-Guide) to learn about how to use the system, the [Appendix](https://github.com/ashwin153/schema/wiki/Appendix) for an exhaustive list of the various library features, and the [Implementation](https://github.com/ashwin153/schema/wiki/Implementation) for more detail about how the system works.

## Release
- Setup GPG: http://macgpg.sourceforge.net/docs/howto-build-gpg-osx.txt.asc
  - Pants configuration isn't compatible with GPG 2.1.
  - Be sure to run ```gpg --keyserver hkp://pgp.mit.edu --send-keys <KEY_ID>```.
- Setup PGP Pants Config: http://www.pantsbuild.org/release_jvm.html#pgp-credentials
- Publish Build Artifacts: ```./pants publish.jar --publish-jar-no-dryrun ::```
  - Pants automatically increments the patch version each release. To change the major or minor versions use ```--publish-jar-override=<GROUP_ID>#<ARTIFACT_ID>=<VERSION>```
- Promote to Maven Central: http://www.pantsbuild.org/release_jvm.html#promoting-to-maven-central
