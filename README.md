# Schema
Schema is a library for executing transactions over arbitrary datastores. Schema provides a Turing complete language for specifying database transactions ([unlike SQL](http://stackoverflow.com/a/900062/1447029)) and utilizes [Multiversion Concurrency Control](https://en.wikipedia.org/wiki/Multiversion_concurrency_control) to optimistically and efficiently execute transactions.

## Build
Library is written in Scala and built using [pants](http://www.pantsbuild.org/).

## Examples
Refer to the [User Guide](/schema/wiki/User-Guide) for more detail.

```scala
Schema { implicit ctx =>
  val x = Select("xid")
  If (exists(x)) {
    Delete(x)
  } Else {
    x.foo = "Hello"
  }
}
```
