![Logo](https://github.com/ashwin153/caustic/blob/master/caustic-assets/images/banner.png)
---
[![Build Status](https://travis-ci.org/ashwin153/caustic.svg?branch=master)][3]
[![Maven Central](https://img.shields.io/maven-central/v/com.madavan/caustic-runtime_2.12.svg)][2]
[![Docker](https://img.shields.io/docker/build/ashwin153/caustic.svg)][4]

Concurrency refers to situations in which multiple programs *concurrently* modify shared data.
Concurrency is challenging because it introduces ambiguity in program execution order - 
programs may be distributed across threads, processes, or even sometimes, in the case of 
__distributed systems__, networks. 

It is precisely this ambituity that causes a particular nasty class of erros known as race 
conditions. A __race condition__ is a situation in which execution order affects the outcome. As a 
motivating example, suppose there exist two programs ```A``` and ```B``` that would like to 
concurrently increment a shared counter ```x```. Formally, each program reads ```x```, sets 
```x' = x + 1```, and writes ```x'```. If ```B``` reads *after* ```A``` finishes writing, then 
```B``` reads ```x'``` and writes ```x' + 1```. However, if ```B``` reads *before* ```A``` finishes 
writing, then ```B``` reads ```x``` and also writes ```x'```. Clearly, this is a race condition 
because the value of the counter, ```x' + 1``` or ```x'```, depends on the order in which ```A``` 
and ```B``` perform reads and writes. This particular race condition may seem relatively benign. Who 
cares if two increments were successfully performed, but the effect of only one was recorded? 
Imagine if the value of ```x``` corresponded to your bank balance, and the increments corresponded 
to deposits. What if your bank only recorded every second deposit? Still don't care? While race 
conditions manifest themselves in subtle ways in concurrent systems, they can often have 
catastrophic consequences.

Most programming languages provide the fundamental tools like locks, semaphores, and monitors to
explicitly deal with race conditions. Some, like Rust, go a step further and are able to detect 
race conditions between concurrent threads during compilation. But none, however, are able to 
*guarantee* correctness in distributed systems. Distributed systems form the computing backbone
of nearly every major technology from social networks to video streaming, but their intricate 
complexity coupled with the inability to detect race conditions makes designing them extremely 
error-prone.
  
__Caustic__ is a programming language for building correct distributed systems. Programs written in
Caustic may be distributed arbitrarily, but they will *never* exhibit race conditions.

# Background
A __transaction__ is a sequence of operations that are atomic, consistent, isolated, and durable. 
Together, these [ACID][6] properties (from which Caustic derives its name!) allow transactions, 
when used correctly, to completely eliminate race conditions.

- __Atomic__: Transactions are all-or-nothing. Either all of their operations complete successfully, 
  or none of them do.
- __Consistent__: Transactions must see the result of all successfully completed transactions.
- __Isolated__: Transactions cannot see the result of an in-progress transaction.
- __Durable__: Transactions are permanent.

A __key-value store__ is a data structure that associates a unique value to any key. For example, a 
dictionary is a key-value store that associates a unique definition to any word. Key-value stores 
are the essence of every storage system; memory is a key-value store that associates a unique 
sequence of bytes to any address, and databases are key-value stores that associate blobs of data to 
any primary key. A __transactional key-value store__ is simply a key-value store that supports 
transactions. While transactions are challenging to correctly implement, there are an enormous 
number of storage systems that are capable of handling them. Examples range from 
[software transaction memory][7] solutions for single machines to powerful databases like 
[Cassandra][8] and [MySQL][9] for larger clusters.

Clearly, transactions are a useful primitive for building correct distributed systems and there 
are a plethora of storage systems capable of handling them. However, these transactional storage 
systems each have their own unique language for specifying transactions that are often lacking in 
functionality and performance. Recent years have marked an explosion in NoSQL databases, that scale
well by shedding functionality. These databases were not popularized because of their query
languages, they were *in spite of them*. Some like [CQL][11] and [AQL][12] attempt to mimic SQL, 
but, while similar in name and intent, most fall short of implementing the entire SQL specification.
Others like [MongoDB][13] and [DynamoDB][14] have their own bespoke interfaces that are often so 
complicated that they require [classes][15]. But even SQL is not beyond reproach. In his article 
["Some Principles of Good Language Design"][10], CJ Date, one of the fathers of relational 
databases, outlined a number of inherent flaws in the SQL language including its lack of a canonical 
implementation and its ambiguous syntax. While these storage systems provide the necessary
transactional guarantees that are required to build correct distributed systems, their lack of a 
robust interface makes it impossible to design nontrivial applications.

Caustic is a powerful and performant programming language for expressing and executing transactions
against *any* transactional key-value store. Caustic couples the robust functionality of a modern 
programming language with the ACID guarantees of a transactional key-value store, both of which are 
necessary to architect correct distributed systems.

# Example
Consider the following example of a distributed counter written in Caustic. It is statically typed
and distributable. It interoperates with any transactional key-value store (currently supports
MySQL, PostgreSQL, and memory), and compiles into a Scala Library that is compatible with all
existing frameworks, tooling, and infrastructure for the JVM. Please refer [here][16] for 
information about how to bootstrap the runtime and [here][17] for information about how to compile
and run Caustic programs.

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

# Organization
```
# Programming Language
caustic/                            https://github.com/ashwin153/caustic
+---caustic-assets/                 Documentation, results, and graphics.
+---caustic-benchmarks/             Performance tests.
+---caustic-compiler/               Programming language and compiler.
+---caustic-runtime/                Virutal machine.

# Syntax Highlighting
caustic.tmbundle/                   https://github.com/ashwin153/caustic.tmbundle
+---Preferences/                    TextMate preferences.
+---Snippets/                       TextMate code snippets.
+---Syntaxes/                       TextMate grammar.

# YCSB Benchmarks
ycsb/                               https://github.com/ashwin153/YCSB
+---caustic                         Caustic integration.
```

# Requirements
- Java 1.8 
- Python 2.7 (build-only) 
- Scala 2.12 

# Artifacts
Artifacts are published to the [Sonatype Nexus][1] and synced to [Maven Central][2]. Snapshots of 
the ```master``` branch are built using [Travis CI][3] and images are available on [Docker][4]. 

```xml
<!-- Client Library -->
<dependency>
  <groupId>com.madavan</groupId>
  <artifactId>caustic-service_2.12</artifactId>
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
[5]: https://blog.rust-lang.org/2015/04/10/Fearless-Concurrency.html
[6]: https://en.wikipedia.org/wiki/ACID
[7]: https://en.wikipedia.org/wiki/Software_transactional_memory
[8]: https://en.wikipedia.org/wiki/Distributed_lock_manager
[9]: https://en.wikipedia.org/wiki/Database_transaction
[10]: https://tinyurl.com/yc7hjvvz
[11]: https://docs.datastax.com/en/cql/3.1/cql/cql_intro_c.html
[12]: https://docs.arangodb.com/3.1/AQL/
[13]: https://www.mongodb.com
[14]: https://aws.amazon.com/dynamodb/
[15]: https://university.mongodb.com/
[16]: https://github.com/ashwin153/caustic/blob/master/caustic-runtime/README.md
[17]: https://github.com/ashwin153/caustic/tree/master/caustic-compiler

