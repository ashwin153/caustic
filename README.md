# Schema
A distributed system is a collection of independent machines that concurrently operate on some shared data model. Transactional key-value store are useful primitives for building distributed systems, because they ensures that these simultaneous operations are transactionally consistent. However, transactional key-value stores come with a few limitations:

1. Transactional key-value stores allow two machines to concurrently modify different key-value pairs. However, it is often the case that multiple machines would like to modify different fields of the same value. No key-value store currently supports this.
2. Because key-value stores may contain values of arbitrary type, it is impossible to know at compile-time the type of the value for a particular key. This vastly complicates the syntax of programs that use them (requires casting), and can potentially lead to fatal errors at runtime when values are not of the type they were expected to be.
3. Transactions are hard and many popular key-value stores either do not support them (Memcached) or do so poorly ([Cassandra](http://stackoverflow.com/a/3007465/1447029)).

The ```schema-core``` library addresses (1) by storing each field of any object as a separate entry in a key-value store to ensure that they are independently modifiable and addresses (2) by leveraging [Shapeless](https://github.com/milessabin/shapeless) to guarantee static type safety of objects stored in the key-value store. The ```schema-distribute``` library addresses (3) by providing a simple way to perform transactional modifications to any key-value store using a variation of multi-word compare-and-swap based on [Tango](http://www.cs.cornell.edu/~taozou/sosp13/tangososp.pdf).

## Schema Distribute
The ```schema-distribute``` library allows transactional modifications to be made on any key-value store by serializing multi-word compare-and-set instructions over an underlying shared [log](https://en.wikipedia.org/wiki/Transaction_log)

### Compare and Swap
Suppose there exists some variable ```x = 0``` that is persisted in some shared storage between two machines. Each machine would like to read the value of ```x```, increment it, and then write the new value back to storage. For example, the first machine would read the value of ```x``` to be ```0```, increment it to ```1```, and then write ```x = 1``` back to storage. However, if the second machine reads the value of ```x``` before the first machine finished writing the new value, it also will read the value of ```x``` to be ```0```, increment it to ```1```, and then write ```x = 1``` back to storage. Clearly, two increments have been performed, but the effect of only one is recorded in storage.

This behavior is incorrect and it is a specific example of a [race condition](https://en.wikipedia.org/wiki/Race_condition), or a situation in which the order of execution affects the outcome of a concurrent operation. In general, this kind of non-deterministic behavior is bad, because it means that a program will have unpredictable behavior. One technique for eliminating race conditions is [compare-and-swap](https://en.wikipedia.org/wiki/Compare-and-swap). According to Wikipedia, compare-and-swap "compares the contents of a memory location to a given value and, only if they are the same, modifies the contents of that memory location to a given new value." In the previous example, each machine would first compare the current value of ```x``` in storage to the value that they read. Only if these values match is the new value of ```x``` written. Clearly, the first increment in the previous example would succeed. However, the second increment would fail, because it is expecting ```x = 0``` but ```x = 1``` after the first increment. Therefore, the value of ```x``` in storage accurately reflects the number of increments that were performed and, consequently, compare-and-swap eliminates the race condition.

### Multi-Word Compare and Swap
We may generalize this compare-and-swap operator to arbitrarily many variables. For example, suppose that you now have two variables ```x``` and ```y``` and you would like to safely increment ```x``` if and only if both ```x``` and ```y``` are unchanged. Each machine first compares the current value of both ```x``` and ```y``` in storage to the value that they read. Only if *both* these values match is the new value of ```x``` written. This procedure is often referred to as multi-word compare-and-swap and it is useful for solving a broad class of problems in distributed systems.

### Transactional Key-Value Store
We may further generalize this procedure of safely incrementing a set of variables, to performing safe modifications to a key-value store. Like ```x``` in the previous example which could be read and written, there are a set of permissible instructions that may be performed on a key-value store. Namely, key-value pairs may be read, inserted, updated, and deleted. By serializing these multi-word compare-and-set operations containing these instructions over a shared log, we can determine which operations succeed by reading the log and performing each operation in order.

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
- [x] ```schema-core/```: Core libraryies
- [x] ```schema-distribute/```: Transactional support for any key-value store.
- [x] ```schema-local/```: ```LocalLog``` and ```LocalSnapshot```; in-memory
- [x] ```schema-memcached/```: ```MemcachedSnapshot```
- [ ] ```schema-kafka/```: ```KafkaLog```
- [x] ```schema-redis/```: ```RedisSnapshot``` and ```RedisManager```
- [ ] ```schema-cassandra/```: ```CassandraSnapshot``` and ```CassandraManager```

## Thanks
- [Tango](http://www.cs.cornell.edu/~taozou/sosp13/tangososp.pdf)
- [Shapeless](https://github.com/milessabin/shapeless)