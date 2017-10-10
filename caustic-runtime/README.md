# Runtime
The ```caustic-runtime``` is responsible for executing transactions on arbitrary key-value stores.

## Terminology
- A ```Transaction``` is an atomic sequence of operations on key-value pairs.
- A ```Database``` is a transactional key-value store.
- A ```Cache``` is a non-transactional key-value store.

## Backends
The runtime currently supports the following databases. Additional implementations will be provided
for other databases in forthcoming releases. Contributions are welcome.

- JDBC
  - ```MySQLDatabase```
  - ```PostgresDatabase```
- Memory
  - ```LocalDatabase```
  - ```LocalCache```
- Redis
  - ```RedisCache```