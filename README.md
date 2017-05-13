# Schema
Schema is a library for expressing and executing database transactions over arbitrary key-value stores. Schema provides a Turing-complete language to **express** transactions ([unlike SQL](http://stackoverflow.com/a/900062/1447029)) and utilizes [Multiversion Concurrency Control](https://en.wikipedia.org/wiki/Multiversion_concurrency_control) to optimistically and efficiently **execute** transactions. Contrast the syntactic difference between the following two equivalent distributed transactions, the first written in [MySQL](https://dev.mysql.com/doc/refman/5.5/en/xa.html) and the second in Schema.

```mysql
XA START 'txn';
INSERT INTO counters (name, count) VALUES ("x", "0") ON DUPLICATE KEY count = count + 1;
XA END 'txn';
XA PREPARE 'txn';
XA COMMIT 'txn';
```
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

## Documentation
Refer to the [User Guide](https://github.com/ashwin153/schema/wiki/User-Guide) to learn about how to use the system, the [Appendix](https://github.com/ashwin153/schema/wiki/Appendix) for an exhaustive list of the various library features, and the [Implementation](https://github.com/ashwin153/schema/wiki/Implementation) for more detail about how the system works.
