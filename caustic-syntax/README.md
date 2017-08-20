# Syntax
The ```caustic-runtime``` package permits transactional operations on arbitrary key-value stores. However, key-value pairs are an extremely unintuitive interface for working with complex object graphs. The ```caustic-syntax``` package introduces a more convenient language for expressing transactions, which relies on the ```caustic-runtime``` to guarantee transactional safety.

# Motivation
1. Garbage collection is important.
2. Object graphs are more intuitive than key-value stores.
3. I/O is expensive.
4. Interoperability is necessary.

# Specification
A top-level record is associated with a user-specified, unique key. Each record has a ```$type``` field which may be ```struct``` or ```attr```. A record of type ```struct``` contains a ```,```-delimited list of field names, and a record of type ```attr``` contains a value. For example, consider the following object model.

```scala
case class Foo(
  bar: Bar
  car: String
)

case class Bar(
  far: Int
)
```

Suppose ```val x = Foo(Bar(1), "Hello")``` is inserted =with unique key ```id```. Then, it would correspond to the following representation in the underlying key-value store.

| Key                         | Value          |
|:----------------------------|:--------------:|
| ```id@$type```              | ```struct```   |
| ```id```                    | ```bar,car,``` |
| ```id@car@$type```          | ```attr```     |
| ```id@car```                | ```"Hello"```  |
| ```id@bar@$type```          | ```struct```   |
| ```id@bar```                | ```far,```     |
| ```id@bar@far@$type```      | ```attr```     |
| ```id@bar@far```            | ```1```        |

## Features
- Because fields of the same object are stored separately, they may be concurrently modified.
- Recursive deletion.
- Canonical json representation.
- Fast traversal by prefetching fields of a ```struct```.
- Minimal additional storage overhead.
