# Syntax
The ```caustic-runtime``` enables operations to be transactionally applied to any key-value store. 

Why is this useful? Inside of every computer is the simplest example of a key-value store - it's called memory. Memory maps each number, or address, to the value stored at the location. Despite this primitive storage system, a computer is capable of running extraordinarily complex programming languages complete with features like objects, variables, functions, and types. How is this possible? Eventually these high-level programming languages compile into a sequence of operations on memory addresses.

The purpose of ```caustic-language``` is to define a high-level programming language, called ```Caustic```, that compiles into a sequence of operations supported by the ```caustic-runtime```. Therefore, programs written in ```Caustic``` may be transactionally run on *any* key-value store. This makes it incredibly easy to perform distributed computation. Say goodbye to locks, semaphores, synchronization, and race conditions. Stop worrying about thread-safety. Let ```Caustic``` take care of that for you.

## Motivation
1. Concurrency is hard.
   - Synchronization is hard to get write, and is a common source of bugs and performance bottlenecks. 
2. Concurrency is important.
   - Synchronization is a necessary component of any distributed system. Parallelization is a huge performance stimulant.
3. Separation of code and data.
   - Code is tightly coupled with the underlying storage engine (ex. rewrite your code to get it to work on different databases, and recompile it for different operating systems, etc.). However, you should be able to *retarget* your code to different storage engines without ever having to change the code itself.

## Specification
### Types
There are four primitive types in Caustic: ```String```, ```Boolean```, ```Integer```, and ```Decimal```. A ```Record``` is the basic type in Caustic. There are three kinds of records: ```Structure```, ```Attribute```, and ```Reference```. Each kind of record contains a different ```value```. A structure contains other records, an attribute contains a primitive value, and a reference points to another record. Structures are returned as json strings, attributes as their equivalent value (eg. ```Flag -> Boolean```), and references as a ```String```. Every record has a ```key```, which uniquely identifies it in the underlying database, and the following attributes:

- ```def key: Transaction```: Record key.
- ```def kind: Transaction```: Record kind. (structure, attribute, or reference)
- ```def value: Transaction```: Record contents. (depends on kind)
- ```def get(name: Transaction): Record```: Field of structure or referenced structure.
- ```def set(value: Transaction): Record```: Update value of record.
- ```def copy(key: Transaction): Record```: Copy the record to the specified location in the database.
- ```def delete(recursive: Boolean): Unit```: Delete the record and its contents, and optionally delete referenced records.

How records get persisted in the database? Recall, the underlying database only knows about three types ```Flag```, ```Real```, and ```Text``` and only understands key-value pairs. In order to persist records into the database, they'll need to be flattened into a map containing only primitive types. Consider the following records described in Caustic.

```
record Foo {
  bar: Bar,
  car: Bar&
}

record Bar {
  far: Integer
}
```

Then, the database might contain the following entries for the record ```Foo(Bar(1), `somekey`)```. 

| Key                          | Value             |
|:-----------------------------|:------------------|
| ```id@__kind__```            | ```"struct"```    |
| ```id```                     | ```"bar,car,"```  |
| ```id@bar@__kind__```        | ```"struct"```    |
| ```id@bar```                 | ```"far,"```      |
| ```id@bar@far@__kind__```    | ```"attr"```      |
| ```id@bar@far```             | ```1```           |
| ```id@car@__kind__```        | ```"ref"```       |
| ```id@car```                 | ```"somekey"```   |

### Variable
In order to scope variables within blocks, Caustic mangles variable names to simulate a stack. The compiler assigns each basic block a unique identifier. Each time a block is entered, the global namespace is set to the concatenation of the current namespace and the block's identifier. Each time a block is exited, the block's identifier is removed from the namespace. All local variable names in a block are prefixed by the global namespace variable (```__namespace__```). 

### Example
```
module caustic.example

import caustic.math._
import caustic.collections.Set

/**
 * Some record.
 *
 * @param x Some param.
 * @param y Some other param.
 * @param z Some other param.
 */
record Foo {
  x: String,
  y: Foo&,
  z: Foo,
}

service Bar {

  def bar(foo: Foo&): Foo = {
    // Last value in a function is returned.
    if (foo.x == "Hello") {
      foo.x = "Goodbye"
      foo
    } else {
      foo.y.x = "Hello"
      foo.y
    }
  }

  def car(foo: Foo&): Foo = {
    // All arguments are passed by value.
    bar(foo)
    bar(foo)
  }

  def fun(): Unit = {
    var x = 0
    while (x < 10) {
      x += 1
    }

    val y = Foo @ "hello"
    if (y.x == "Hello")
      rollback y
}
```