# Runtime
The ```caustic-runtime``` provides a cross-language service for optimistically executing transactions over arbitrary key-value stores.

## Features
- **Minimal I/O**: Guarantees that it makes the *fewest number of I/O operations in order to execute a transaction. It does this by batch reads and writes of a transaction and performing as many as possible at a time. Optimizations include prefetching the read and write sets of both branches of a conditional and the body of a loop and performing all writes together at the end of execution.
- **Interoperable**: Exposes a Thrift RPC interface for submitting transactions for execution.
- **Tail recursive**: This means that it is capable of processing massive syntax-trees that might otherwise cause a stack overflow.
- **Strongly Typed**: Execution is strongly-typed. This allows it to produce more meaningful error messages than would be otherwise possible.