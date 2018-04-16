![Logo](https://github.com/ashwin153/caustic/blob/master/caustic-assets/images/caustic-banner.png)
---
[![Build Status](https://travis-ci.org/ashwin153/caustic.svg?branch=master)][3]
[![Maven Central](https://img.shields.io/maven-central/v/com.madavan/caustic-runtime_2.12.svg)][2]
[![Docker](https://img.shields.io/docker/build/ashwin153/caustic.svg)][4]

Concurrency is *hard*. Concurrency refers to situations in which multiple programs
simultaneously modify shared data. Concurrent programs may be run across threads, processes,
and, in the case of distributed systems, networks. Concurrency is challenging because it
introduces ambiguity in execution order, and it is precisely this ambiguity that causes a class of
failures known as race conditions.

Race conditions may occur whenever the order in which concurrent programs are executed affects their
outcome. For example, suppose there exist two programs ```A``` and ```B``` that each increment a 
shared counter ```x```. Formally, each program reads the current value of ```x``` and then writes 
```x + 1```. If ```B``` reads *after* ```A``` writes, then ```B``` reads ```x + 1``` and writes 
```x + 2```. However, if ```B``` reads *before* ```A``` writes but after ```A``` reads, then both 
```A``` and ```B``` will read ```x``` and write ```x + 1```. This is an example of a race condition, 
because the value of the counter ```x``` after both ```A``` and ```B``` have completed depends on 
the order in which ```A``` and ```B``` are executed. This race condition may seem relatively benign, 
but it can have catastrophic consequences in practical systems. Suppose the value of ```x``` 
corresponded to your bank balance. What if your bank determined your balance differently depending 
on the order in which deposits are made? Race conditions manifest themselves in subtle ways in 
concurrent systems, and they can often be difficult to detect and challenging to remove.

Most programming languages provide the fundamental tools like locks, semaphores, and monitors to
explicitly deal with race conditions. Some, like [Rust][5], go a step further and are able to detect 
race conditions between concurrent threads during compilation. But none, however, are able to 
*guarantee* correctness in distributed systems. Distributed systems form the computing backbone
of nearly every major technology from social networks to video streaming, but their intricate 
complexity coupled with the inability to detect race conditions makes designing them extremely 
error-prone.
  
Concurrency is difficult, but it does not need to be. Caustic allows programmers to build concurrent
systems as if they were they were single-threaded. Programs written in Caustic may be distributed 
arbitrarily, but they will *never* exhibit race conditions. Caustic is a highly programmable 
alternative to traditional approaches for dealing with race conditions. It features concise syntax, 
static typing, aggressive type inference, and a performant runtime. Consider the following example 
of a distributed counter written in Caustic. It interoperates with any transactional key-value 
store, and compiles into a Scala Library that is compatible with all existing JVM frameworks, 
tooling, and infrastructure.

```
module caustic.example

/**
 * A count.
 *
 * @param value Current value.
 */
struct Total {
  value: Int
}

/**
 * A distributed counter.
 */
service Counter {

  /**
   * Increments the total and returns its current value.
   *
   * @param x Reference to total.
   * @return Current value.
   */
  def increment(x: Total&): Int = {
    if (x.value) x.value += 1 else x.value = 1
    x.value
  }

}
```

# Structure
```
# Programming Language
caustic/                            https://github.com/ashwin153/caustic
+---build-support/                  Pants plugins and configuration.
+---caustic-assets/                 Documentation, results, and graphics.
+---caustic-compiler/               Programming language.
+---caustic-example/                Example programs.
+---caustic-library/                Standard library.
+---caustic-runtime/                Virutal machine.
```

# Requirements
- Java 1.8 
- Python 2.7 (build-only) 
- Scala 2.12 

# Artifacts
Artifacts are published to the [Sonatype Nexus][1] and synced to [Maven Central][2]. Snapshots of 
the ```master``` branch are built using [Travis CI][3] and images are available on [Docker][4]. 

```xml
<!-- Compiler Library -->
<dependency>
  <groupId>com.madavan</groupId>
  <artifactId>caustic-compiler_2.12</artifactId>
  <version>1.5.5</version>
</dependency>

<!-- Standard Library -->
<dependency>
  <groupId>com.madavan</groupId>
  <artifactId>caustic-library_2.12</artifactId>
  <version>1.5.5</version>
</dependency>

<!-- Runtime Library -->
<dependency>
  <groupId>com.madavan</groupId>
  <artifactId>caustic-runtime_2.12</artifactId>
  <version>1.5.5</version>
</dependency>
```

[1]: https://oss.sonatype.org/index.html#nexus-search;quick~com.madavan
[2]: https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.madavan%22
[3]: https://travis-ci.org/ashwin153/caustic
[4]: https://hub.docker.com/r/ashwin153/caustic/
[5]: https://blog.rust-lang.org/2015/04/10/Fearless-Concurrency.html