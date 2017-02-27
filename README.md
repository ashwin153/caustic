# Schema
Traditionally, distributed systems rely on complicated protocols such as [Two-Phase Commit](https://en.wikipedia.org/wiki/Two-phase_commit_protocol) to perform transactional modifications to shared object models. These protocols are difficult to write, slow to run, and cumbersome to use. Schema enables the architects of distributed systems to write simple, scalable, and elegant code without sacrificing transactional consistency.

## Schema Distribute
The ```schema-distribute``` library allows transactional modifications to be made on any key-value store by serializing multi-word compare-and-set instructions over an underlying shared ```Log```.

Suppose there exists some variable ```x = 0``` that is persisted in some shared storage between two machines. Each machine would like to read the value of ```x```, increment it, and then write the new value back to storage. For example, the first machine would read the value of ```x``` to be ```0```, increment it to ```1```, and then write ```x = 1``` back to storage. However, if the second machine reads the value of ```x``` before the first machine finished writing the new value, it also will read the value of ```x``` to be ```0```, increment it to ```1```, and then write ```x = 1``` back to storage. Clearly, two increments have been performed, but the effect of only one is recorded in storage.

This behavior is incorrect and it is a specific example of a race condition, or a situtation in which the order of execution affects the outcome of a concurrent operation. In general, this kind of non-deterministic behavior is bad, because it means that a program will have unpredictable behavior. One technique for eliminating race conditions is [compare-and-swap](https://en.wikipedia.org/wiki/Compare-and-swap). According to Wikipedia, compare-and-swap "compares the contents of a memory location to a given value and, only if they are the same, modifies the contents of that memory location to a given new value." In the previous example, each machine would first compare the current value of ```x``` in storage to the value that they read. Only if these values match is the new value of ```x``` written. Clearly, the first increment in the previous example would succeed. However, the second increment would fail, because it is expecting ```x = 0``` but ```x = 1``` after the first increment. Therefore, the value of ```x``` in storage accurately reflects the number of increments that were performed and, consequently, compare-and-swap eliminates the race condition.

We may generalize this compare-and-swap operator to arbitrarily many variables. For example, suppose that you now have two variables ```x``` and ```y``` and you would like to safely increment ```x``` if and only if both ```x``` and ```y``` are unchanged. Each machine first compares the current value of both ```x``` and ```y``` in storage to the value that they read. Only if *both* these values match is the new value of ```x``` written. This procedure is often referred to as multi-word compare-and-swap and it is useful for solving a broad class of problems in distributed systems.

### Transactions
Suppose that instead of a variable ```x``` we have some key-value store that is distributed across multiple machines. We would like to apply the same principle of multi-word compare-and-swap that we used to perform safe increments to ```x``` to this distributed key-value store. Like ```x```, which could be read and written, there are a set of permissible instructions that may be performed on a key-value store. Namely, key-value pairs may be read, inserted, updated, and deleted. By serializing these multi-word compare-and-set operations over a shared log and reading them back in order, we can determine which operations succeed and which fail.

## Schema Core
A [key-value store](https://en.wikipedia.org/wiki/Key-value_database) is a set of mappings between unique keys and arbitrary values. For example, a key-value store might consist of the mappings ```{ "foo" -> 123, "bar" -> "abc" }```. While key-value stores are highly scalable and simple to use, the previous example highlights one of their most important shortcomings - lack of static type information. Because key-value contain values of arbitrary type, it is impossible to know at compile-time the type of the value for a particular key. This complicates the syntax of programs that use them (dynamic casting), and can potentially lead to fatal errors at runtime when values are not of the type there were expected to be.

The ```schema-core``` library allows the fields of arbitrary objects to be independently stored in a key-value store while retaining static type safety. Because each field is stored as a separate entry in the key-value store, it is possible to transactionally modify different fields of the same object *at the same time*. This should significantly improve the write-throughput of distributed, transactional key-value stores.

## Examples
Suppose you have the following data model that is distributed across some number of machines.

```scala
case class Foo(bar: String)
```

On each machine, construct a ```Manager``` that is backed by a shared ```Log```. That's it!

```scala
this.manager.txn { schema =>
  schema.select[Foo]("myid") match {
    case Some(x) =>
      x('bar) = "update"
      Commit()
    case None => 
      schema.insert("myid", Foo("test"))
      Commit()
  }
}
```

## Overview
The main libraries are:
- [x] ```schema-core/```: Static typing of objects whose fields are independently stored in a ```Snapshot```. Because fields are stored as separate key-value pairs, different fields of the same object may be concurrently modified which significantly improves the write throughput of the system.
- [x] ```schema-distribute/```: Transactional modification of any distributed ```Snapshot```. Transactions performed on the ```Snapshot``` are serialized as compare-and-swap instructions to an underlying shared ```Log``` to resolve write conflicts. Transactional modifications are lock-free and highly scalable.

A variety of ```Snapshot```, ```Log```, and ```Manager``` implementations are also provided:
- [x] ```schema-local/```: ```LocalLog``` and ```LocalSnapshot```; in-memory
- [x] ```schema-memcached/```: ```MemcachedSnapshot```
- [ ] ```schema-kafka/```: ```KafkaLog```
- [x] ```schema-redis/```: ```RedisSnapshot``` and ```RedisManager```
- [ ] ```schema-cassandra/```: ```CassandraSnapshot``` and ```CassandraManager```

## Thanks
- [Tango](http://www.cs.cornell.edu/~taozou/sosp13/tangososp.pdf)
- [Shapeless](https://github.com/milessabin/shapeless)