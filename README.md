# Schema
Schema is a library for expressing and executing database transactions. Schema provides a dynamically-typed language to **express** transactions and utilizes [Multiversion Concurrency Control](https://en.wikipedia.org/wiki/Multiversion_concurrency_control) to optimistically and efficiently **execute** transactions on *arbitrary* key-value stores. The following is a distributed transaction written in Schema to give you a taste of what the language can do.

```scala
Schema { implicit ctx =>
  val counter = Select("x")
  If (!counter) {
    counter.count = 0
  } Else {
    counter.count += 1
  }
}
```

## Overview
- ```schema-runtime/```: Core runtime library
- ```schema-benchmark/```: Performance tests
- ```schema-mysql/```: MySQL integration
- ```schema-postgresql/```: PostgreSQL integration

## Documentation
Refer to the [User Guide](https://github.com/ashwin153/schema/wiki/User-Guide) to learn about how to use the system, the [Appendix](https://github.com/ashwin153/schema/wiki/Appendix) for an exhaustive list of the various library features, and the [Implementation](https://github.com/ashwin153/schema/wiki/Implementation) for more detail about how the system works.

## Schedule
- [ ] Test the ```syntax/``` package
- [ ] Cut an alpha release (6/1/17)
- [ ] Write better examples
- [ ] Design a generic conditional put implementation
- [ ] Add support for more databases
- [ ] Cut a beta release (7/1/17)
- [ ] Benchmark and stress test
- [ ] Create sample starter projects
- [ ] Cut a 1.0 release (8/1/17)
