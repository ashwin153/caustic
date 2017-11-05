![Logo](https://github.com/ashwin153/caustic/blob/master/caustic-assets/images/banner.png)
---
[![Build Status](https://travis-ci.org/ashwin153/caustic.svg?branch=master)][3]
[![Maven Central](https://img.shields.io/maven-central/v/com.madavan/caustic-runtime_2.12.svg)][2]
[![Docker](https://img.shields.io/docker/build/ashwin153/caustic.svg)][4]

Concurrency is hard. Why waste time worrying about it? Languages like [Rust][5] are capable of 
eliminating race conditions between multiple threads operating on shared data in a single machine, 
but they do little to guarantee safety between multiple *machines* in a cluter. Therefore, 
programmers are forced to rely on explicit synchronization to coordinate operations in a distributed 
system. __Caustic is a transactional programming language for distributed systems.__ Caustic 
programs may be distributed arbitrarily and executed concurrently without *any* explicit 
synchronization against *any* transactional key-value store.

## Background
A transaction is a sequence of operations that are atomic, consistent, isolated, and durable.

- __Atomic__: Transactions are all-or-nothing. In other words, a transaction is an atom of 
  computation - it cannot be split apart. Either all the operations in a transaction are performed 
  successfully, or none of them are.
- __Consistent__: Once a transaction has completed successfully, all future transactions must see 
  its result.
- __Isolated__: Transactions do not reveal intermediate results.
- __Durable__: Once a transaction has completed successfully, its result will remain permanently 
  recorded *even in the event of power loss, crashes, or errors*.

A race condition is a situation in which the order of operations affects the result. As a motivating 
example, suppose there exist two machines ```A``` and ```B``` that each would like to increment a 
shared counter ```x```. Each machine (1) reads the value of ```x```, (2) adds ```1``` to it, and 
(3) writes ```x + 1```. If ```B``` reads *after* ```A``` finishes writing, then ```B``` reads 
```x + 1``` and writes ```x + 2```. If ```B``` reads *before* ```A``` finishes writing, then ```B``` 
also reads ```x``` and also writes ```x + 1```. Clearly, this is a race condition because the value
of ```x``` depends on the *order* in which the increments are applied. This particular race 
condition may seem relatively benign. Who cares if two increments were performed, but only one was 
recorded? Imagine if the value of ```x``` corresponded to your bank balance, and the increments 
corresponded to deposits. What if your bank only recorded every second deposit? Race conditions 
manifest themselves in subtle ways in distributed systems, and are difficult to discover and 
eliminate. 

These [ACID][6] properties (from which Caustic derives its name!) make transactions a formidible 
tool for eliminating race conditions in distributed systems. If the machines in the previous example
had *transactionally* written the new value of ```x``` *if and only if the value of ```x``` remained 
unchanged*, then whenever ```B``` read *before* ```A``` finished writing ```B``` would detect
the modification made to ```x``` by ```A``` when it attempted to write ```x + 1``` and would fail to 
complete successfully. Because the value of ```x``` depends only on the *number* of successful 
increments and not on the *order* in which they were applied, the race condition has been 
eliminated.

A key-value store is the simplest kind of data structure - it asssociates a unique value to any key.
For example, a dictionary is a key-value store that associates a unique definition to any word. 
Key-value stores are the essence of every storage system; memory is a key-value store that 
associates a unique sequence of bytes to any address, and databases are key-value stores that 
associate blobs of data to any primary key.

A transactional key-value store is simply a key-value store that supports transactions. While the
ACID properties of transactions are extremely challenging to guarantee, there are an enourmous 
number of system that do. Examples range from [software transaction memory][7] solutions for single
machines to powerful databases like [Cassandra][8] and [MySQL][9] for distributed clusters.

Clearly, transactions are a useful primitive for architecting correct distributed systems and there 
are a plethora of storage systems capable of handling them. However, these storage systems each have
their own language for specifying transactions that are often lacking in functionality and 
performance. Caustic provides a powerful and performant language for expressing and executing 
transactions against *any* transactional key-value store. 

## Example
Consider the following example of a distributed counter written in Caustic. It is statically typed,
distributable, and interoperable with any transactional key-value store. It compiles into a Scala
library and so it is compatible with existing frameworks, tooling, and infrastructure for the JVM.

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
[5]: https://www.rust-lang.org/en-US/
[6]: https://en.wikipedia.org/wiki/ACID
[7]: https://en.wikipedia.org/wiki/Software_transactional_memory
