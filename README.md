# Schema
Suppose that you have some variable ```x = 0``` that is persisted in some shared storage and used by two machines. Each machine would like to read the value of ```x```, increment it, and then write the new value of ```x``` back to storage. For example, suppose that the first machine reads the value of ```x``` to be ```0```, it then increments it to ```1```, and then writes ```x = 1``` back to storage. However, if the second machine reads the value of ```x``` before the first machine finishes writing the new value, it will also read the value of ```x``` to be ```0```, perform an increment, and attempt to write ```x = 1``` back to storage. Therefore, two increments to ```x``` have been performed but ```x = 1``` in storage. This is a classic example of a race condition, or a situation in which the order of execution affects the outcome of a concurrent operation. In general, this kind of non-deterministic behavior is bad because it means that a program will have unpredictable behavior. One technique for preventing race conditions is [compare-and-swap](https://en.wikipedia.org/wiki/Compare-and-swap). According to Wikipedia, compare-and-swap "compares the contents of a memory location to a given value and, only if they are the same, modifies the contents of that memory location to a given new value." In the previous example, each machine would first compare the current value of ```x``` in storage to the value that they read. Only when these values match, is the new value of ```x``` written. Therefore, the second write will fail to be applied because ```x = 1``` after the first write is applied. Clearly, compare-and-swap eliminates the race condition and ensures that the value of ```x``` can be safely incremented.

Now suppose that instead of a single number ```x``` these two machines share a ```Snapshot```, or [key-value store](https://en.wikipedia.org/wiki/Key-value_database). A ```Snapshot``` is a mapping from some unique key to any arbitrary value. Like ```x``` which may be read and incremented, there exists a set of instructions that may be performed on the ```Snapshot```. Namely, key-value pairs may be read, updated, and deleted. Each time a group of instructions, called a ```Transaction``` is performed on a ```Snapshot``` they are translated into compare-and-swap instructions and persisted to some shared ```Log```. These transactions are then read back in order, and those that are successful are applied to the ```Snapshot```.

In this way, we can construct a distributed ```Snapshot``` that is transactionally modified. However, this ```Snapshot``` is extremely cumbersome to use, precisely because it is so generalized. Because a ```Snapshot``` may store values of arbitrary type, values must be cast to their expected type at runtime. Not only does this significantly complicate the syntax of a program, it also can lead to fatal errors at runtime when values are not of the type they were expected to be at compile-time. Therefore, I invented ```Schema```, which provides a method of independently accessing and modifying the fields of statically typed objects backed by an underlying ```Snapshot```. This means that you can now transactionally modify *any* distributed object model, without sacrificing static type-safety.

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
A variety of common ```Snapshot``` and ```Log``` implementations are provided.

- [x] ```schema-core/```: Core library
- [x] ```schema-local/```: ```LocalLog``` and ```LocalSnapshot```; in-memory
- [ ] ```schema-kafka/```: ```KafkaLog```
- [ ] ```schema-redis/```: ```RedisSnapshot```
- [ ] ```schema-memcached/```: ```MemcachedSnapshot```

## Thanks
- [Tango](http://www.cs.cornell.edu/~taozou/sosp13/tangososp.pdf)
- [Shapeless](https://github.com/milessabin/shapeless)