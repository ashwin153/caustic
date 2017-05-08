# Schema
Schema is a library for expressing and executing database transactions over arbitrary key-value stores. Schema provides a Turing-complete language to **express** transactions ([unlike SQL](http://stackoverflow.com/a/900062/1447029)) and utilizes [Multiversion Concurrency Control](https://en.wikipedia.org/wiki/Multiversion_concurrency_control) to optimistically and efficiently **execute** transactions. Contrast the syntactic difference between the following two equivalent distributed transactions, the first written in [MySQL](https://dev.mysql.com/doc/refman/5.5/en/xa.html) and the second in Schema.

```mysql
XA START 'txn';
UPDATE posts SET status='approved' where post_id='postId' AND status != 'approved';
XA END 'txn';
XA PREPARE 'txn';
XA COMMIT 'txn';
```

```scala
Schema { implicit ctx =>
  val post = Select("postId")
  If (post.status != "approved") {
    post.status = "approved"
  }
}
```

## Documentation
Refer to the [User Guide](https://github.com/ashwin153/schema/wiki/User-Guide) for more detail about how to use the system and the [Implementation](https://github.com/ashwin153/schema/wiki/Implementation) for more detail about how the system works.
