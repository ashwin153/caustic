# Running
- On Amazon EC2, the JVM heap size must be configured.
```bash
./pants run caustic-benchmark/src/main/scala:bin \
  --jvm-run-jvm-options='-Xms2048M -Xmx4096M'               // JVM heap size.
  -- throughput                                             // Name of benchmark.
```