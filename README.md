# Schema
Distributed transactions.

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
