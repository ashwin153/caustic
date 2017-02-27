# Schema
Suppose that you have some variable ```x = 0``` that is persisted in some shared storage and used by two machines. Each machine would like to read the value of ```x```, increment it, and then write the new value of ```x``` back to storage. For example, suppose that the first machine reads the value of ```x``` to be ```0```, it then increments it to ```1```, and then writes ```x = 1``` back to storage. However, if the second machine reads the value of ```x``` before the first machine finishes writing the new value, it will also read the value of ```x``` to be ```0```, perform an increment, and attempt to write ```x = 1``` back to storage. Therefore, two increments to ```x``` have been performed but ```x = 1``` in storage. Clearly, this behavior is incorrect, and it is a classic example of a race condition, or a situation in which the order of execution affects the outcome of a concurrent operation. In general, this kind of non-deterministic behavior is bad because it means that a program will have unpredictable behavior. One technique for preventing race conditions is [compare-and-swap](https://en.wikipedia.org/wiki/Compare-and-swap). According to Wikipedia, compare-and-swap "compares the contents of a memory location to a given value and, only if they are the same, modifies the contents of that memory location to a given new value." In the previous example, each machine would first compare the current value of ```x``` in storage to the value that they read. Only when these values match, is the new value of ```x``` written. Therefore, the second write will fail to be applied because it is expecting ```x = 0``` but ```x = 1``` after the first write succeeds. Clearly, compare-and-swap eliminates the race condition and ensures that the value of ```x``` can be safely incremented.

We can generalize this procedure for safely incrementing some shared variable ```x``` to safely modifying some shared ```Snapshot```, or [key-value store](https://en.wikipedia.org/wiki/Key-value_database). A ```Snapshot``` is a set of mappings between unique keys and arbitrary values. For example, a ```Snapshot``` might consist of the mappings ```{ "foo" -> 123, "bar" -> "abc" }```. Like ```x```, which could be read and incremented, there are a set of instructions that may be performed on a ```Snapshot```. Namely, key-value mappings in a ```Snapshot``` may be inserted, read, updated, and deleted. Each time a group of instructions, called a ```Transaction```, is attempted on a ```Snapshot``` they are first translated into compare-and-swap instructions and persisted to some shared storage, called a ```Log```. These transactions are then read back in order, and those that are successful are applied to the ```Snapshot```.

In this manner, we can construct a distributed, shared ```Snapshot``` that is transactionally modifiable. However, this ```Snapshot``` is extremely cumbersome to use, precisely because it is so generalized. Because a ```Snapshot``` may store values of arbitrary type, values must be cast to their expected type at runtime in order to be used. Not only does this significantly complicate the syntax of a program, but it also can lead to fatal errors at runtime when values are not of the type they were expected to be at compile-time. Therefore, I invented ```Schema```, which provides a method of independently accessing and modifying the fields of statically typed objects stored in an underlying ```Snapshot```. This means that the fields of any object stored in a distributed, shared ```Snapshot``` may be transactionally modified without sacrificing static type safety.

Traditionally, distributed systems rely on complicated protocols such as [Two-Phase Commit](https://en.wikipedia.org/wiki/Two-phase_commit_protocol) to perform transactional modifications to shared object models. These protocols are difficult to write, slow to run, and cumbersome to use. Schema enables the architects of distributed systems to write simple, scalable, and elegant code without sacrificing transactional consistency.

## Examples
Suppose you have the following data model that is distributed across some number of machines.

```scala
case class Foo(bar: String)=
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