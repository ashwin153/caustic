# Syntax
The ```caustic-runtime``` package permits transactional operations on arbitrary key-value stores. However, key-value pairs are an extremely unintuitive interface for working with complex object graphs. The ```caustic-syntax``` package introduces a more convenient language for expressing transactions, which relies on the ```caustic-runtime``` to guarantee transactional safety.

# Specification
Every key is associated with a ```__type__``` field which may be ```struct``` or ```attr```. A value of a key of type ```struct``` contains a ```,```-delimited list of field names, and the value of key of type ```attr``` corresponds to the value of the field identified by the key. For example, consider the following object model.

```scala
case class Foo(
  bar: Bar
  car: String
)

case class Bar(
  far: Int
)
```

Suppose instance ```val x = Foo(Bar(1), "Hello")``` is inserted into the underlying key-value store with key ```id```. Then, it would correspond to the following representation.

| Key                         | Value          |
|:----------------------------|:--------------:|
| ```id@__type__```           | ```struct```   |
| ```id```                    | ```bar,car,``` |
| ```id@car@__type__```       | ```attr```     |
| ```id@car```                | ```"Hello"```  |
| ```id@bar@__type__```       | ```struct```   |
| ```id@bar```                | ```far,```     |
| ```id@bar@far@__type__```   | ```attr```     |
| ```id@bar@far```            | ```1```        |
