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
  If (Exists(counter)) {
    counter.count = 0
  } Else {
    counter.count += 1
  }
}
```

## Schema-As-A-Service
The ultimate goal of this project is to host a massive, multi-tenant database that customers may use to store their data (BaaS). Customers will use this library to express and execute transactions on their data. Schema gives its customers all the benefits that come with operating at scale (geo-replication, better hardware, faster networks, high-performance caching, etc.) right from day one. Schema charges based on the server resources (network, disk, cpu) consumed by the customer. This ensures the cost of Schema scales with the size of the company. Schema allows customers to choose the cluster on which they store their data. Different clusters would support different workloads (read-heavy, write-heavy, and mixed) and would have varying tiers of hardware quality. Customers would pay depending on their workload (reads are cheap on read-heavy clusters but writes are expensive, etc.) and on the quality of their hardware (better the hardware, higher the price).

## Documentation
Refer to the [User Guide](https://github.com/ashwin153/schema/wiki/User-Guide) for more detail about how to use the system and the [Implementation](https://github.com/ashwin153/schema/wiki/Implementation) for more detail about how the system works.
