# Benchmark
Performance benchmarks were conducted on a t2.xlarge instance with 32GB of RAM and 8 CPUs. When
running the benchmarking suite the JVM heap size must be manually configured to avoid triggering the
garbage collector or an ```OutOfMemoryError```. This can be done in the following manner. We
recommend setting the minimum heap size to at least 2 GB. 

```bash
./pants run caustic-benchmark/src/main/scala:bin \
  --jvm-run-jvm-options='-Xms2048M -Xmx4096M' \             // JVM heap size.
  -- throughput                                             // Name of benchmark.
```