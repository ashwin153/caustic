# Benchmark
Performance benchmarks were conducted on a t2.xlarge instance with 32GB of RAM and 8 CPUs and
profiling was conducting using [JProfiler][1]. When running the benchmarking suite the JVM heap 
size must be manually configured to avoid triggering the garbage collector or an 
```OutOfMemoryError```. This can be done in the following manner. We recommend setting the 
minimum heap size to at least 2 GB. 

```bash
./pants run caustic-benchmark/src/main/scala:bin \
  --jvm-run-jvm-options='-Xms2048M -Xmx4096M' \             // JVM heap size.
  -- throughput                                             // Name of benchmark.
```

## Latency
![Latency][2]

In this benchmark, we generate programs consisting of the specified number of read expressions and
measure the time it takes for the program to commit. We see that latency grows linearly with program
size. In other words, *each additional expression has a constant overhead*. 

## Throughput
![Throughput][3]

In this benchmark, the specified number of threads simultaneously execute a series of programs that
each read and write the specified percentages of the key-space. Note that the larger the percentages
of reads and writes and the greater the number of threads, the greater the likelihood that
transactions will fail and the more significant the reduction in throughput will be. We see that in
read-only workloads, throughput scales linearly with the number of threads. As the percentage of
reads and writes and the number of threads increases, throughput falls considerably but still scales
linearly with the number of threads.

[1]: https://www.ej-technologies.com/products/jprofiler/overview.html
[2]: https://raw.githubusercontent.com/ashwin153/caustic/master/caustic-assets/images/runtime-latency.png
[3]: https://raw.githubusercontent.com/ashwin153/caustic/master/caustic-assets/images/runtime-throughput.png
