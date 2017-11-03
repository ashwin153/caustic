![Logo](https://github.com/ashwin153/caustic/blob/master/caustic-assets/images/banner.png)
---
[![Build Status](https://travis-ci.org/ashwin153/caustic.svg?branch=master)][3]
[![Maven Central](https://img.shields.io/maven-central/v/com.madavan/caustic-runtime_2.12.svg)][2]
[![Docker](https://img.shields.io/docker/build/ashwin153/caustic.svg)][4]

Concurrency is hard. Why waste time worrying about it? Caustic is a transactional programming
language for arbitrary key-value stores. Caustic programs may be distributed arbitrary and
executed concurrently without *any* explicit synchronization mechanism, and they may be run
without modification on *any* transactional key-value store.

Consider the following example of a distributed counter written in Caustic. It may be short,
simple, and terse, but it is also thread-safe, performant, and interoperable with any 
transactional key-value store (including MySQL, PostgreSQL, and main memory).

```
module counter

/**
 * A counter.
 *
 * @param value Current value.
 */
record Total {
  value: Int
}

/**
 * A distributed counting service.
 */
service Counter {

  /**
   * Increments the total and returns its current value.
   *
   * @param x Total pointer.
   * @return Current value.
   */
  def increment(x: Total&): Int = {
    if x.value {
      x.value += 1
    } else {
      x.value = 1
    }
  }

}
```

## Overview
- ```caustic-assets```: Pictures, documentation, and musings.
- ```caustic-benchmarks```: Performance tests.
- ```caustic-compiler```: Programming language.
- ```caustic-runtime```: Virtual machine.

## Requirements
- Java 1.8
- MySQL 5.0+
- PostgreSQL 9.5+
- Python 2.7
- Scala 2.12
- ZooKeeper 3.4.10

## Artifacts
Artifacts are published to the [Sonatype Nexus][1] and synced to 
[Maven Central][2]. Snapshots of the ```master``` branch are built using [Travis CI][3] and images
are available on [Docker][4]. The Maven coordinates of core build artifacts are as follows.

```xml
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

[1]: https://oss.sonatype.org/index.html#nexus-search;quick~com.madavan
[2]: https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.madavan%22
[3]: https://travis-ci.org/ashwin153/caustic
[4]: https://hub.docker.com/r/ashwin153/caustic/
