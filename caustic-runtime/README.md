# Runtime
The runtime is a virtual machine that dynamically compiles __programs__ into __transactions__ that 
are *atomically* and *consistently* executed in *isolation* on any *durable*, transactional 
key-value store, called a __volume__. A program is an abstract-syntax tree that is composed of
__literals__ and __expressions__. A literal is a scalar value of type ```flag```, ```real```, 
```text```, or ```null``` which correspond to bool, double, string, and null respectively in most 
C-style languages. An expression is a function that transforms literal arguments into a literal 
result. Expressions may be chained together arbitrarily to form complex programs.

| Expression               | Description                                                           |
|:-------------------------|:----------------------------------------------------------------------|
| ```add(x, y)```          | Sum of ```x``` and ```y```.                                           | 
| ```both(x, y)```         | Bitwise AND of ```x``` and ```y```.                                   |
| ```branch(c, p, f)```    | Executes ```p``` if ```c``` is true, or ```f``` otherwise.            | 
| ```cons(a, b)```         | Executes ```a``` and then ```b```.                                    | 
| ```contains(x, y)```     | Returns whether ```x``` contains ```y```.                             |
| ```cos(x)```             | Cosine of ```x```.                                                    |
| ```div(x, y)```          | Quotient of ```x``` and ```y```.                                      |
| ```either(x, y)```       | Bitwise OR of ```x``` and ```y```.                                    |
| ```equal(x, y)```        | Returns whether ```x``` and ```y``` are equal.                        |
| ```floor(x)```           | Floor of ```x```.                                                     |
| ```indexOf(x, y)```      | Returns the index of the first occurrence of ```y``` in ```x```.      | 
| ```length(x)```          | Returns the number of characters in ```x```.                          |
| ```less(x, y)```         | Returns whether ```x``` is strictly less than ```y```.                |
| ```load(n)```            | Loads the value of the variable ```n```.                              |
| ```log(x)```             | Natural log of ```x```.                                               |
| ```matches(x, y)```      | Returns whether or not ```x``` matches the regex pattern ```y```.     |
| ```mod(x, y)```          | Remainder of ```x``` divided by ```y```.                              |
| ```mul(x, y)```          | Product of ```x``` and ```y```.                                       | 
| ```negate(x)```          | Bitwise negation of ```x```.                                          |
| ```pow(x, y)```          | Returns ```x``` raised to the power ```y```.                          | 
| ```prefetch(k, s)```     | Reads keys at ```k/i``` for ```i``` in ```[0, s)```.                  |
| ```read(k)```            | Reads the value of the key ```k```.                                   |
| ```repeat(c, b)```       | Repeatedly executes ```b``` while ```c``` is true.                    |
| ```rollback(r)```        | Discards all writes and returns ```r```.                              |
| ```sin(x)```             | Sine of ```x```.                                                      | 
| ```slice(x, l, h)```     | Returns the substring of ```x``` between ```[l, h)```.                |
| ```store(n, v)```        | Stores the value ```v``` for the variable ```n```.                    |
| ```sub(x, y)```          | Difference of ```x``` and ```y```.                                    |
| ```write(k, v)```        | Writes the value ```v``` for the key ```k```.                         |

## Execution
The runtime uses optimistic concurrency to serialize program execution over the underlying volume.
Optimistic concurrency permits multiple programs to simultaneously access, but not modify, shared
data without acquiring locks. Each program locally buffers any modifications that it makes to shared
data and attempts to atomically apply them when it completes conditioned on the data that it 
accessed remaining unchanged. If any data was modified, the program retries. This conditional
update, known as a [multi-word compare-and-swap][2] and hereafter referred to as cas, is known 
to be transactional and is widely used in a number of software transactional memory systems. 

Optimistic concurrency requires a mechanism to detect that data has changed. The approach taken in 
Caustic and in similar systems is known as [multi-version concurrency control][1]. Data is uniquely 
identified by a __key__ and is encapsulated in a __revision__, or versioned value, whose version is 
incremented each time that its value is changed. We say that revisions *A* and *B* of a key 
__conflict__ if the version of *A* is less than that of *B*. We will assume that the underlying
volume correctly implements __get__, which returns the revisions of a set of keys, and __cas__, 
which updates the revisions of a set of keys if and only if the revisions of a set of dependent keys 
do not conflict.

The runtime uses iterative partial evaluation to gradually reduce programs into a single literal
result according to the following procedure.

1. __Fetch__: Get all keys that are read or written anywhere in the program that have not been 
   fetched before and add the returned revisions to a local __snapshot__.
2. __Evaluate__: Recursively replace all expressions with literal arguments with their corresponding 
   literal result. For example, ```add(real(1), sub(real(0), real(2))) = real(-1)```. The result of 
   all writes is saved to a local __buffer__ and the result of all reads is the latest value of the 
   key in the local buffer or snapshot. This ensures that reads will see the effect of all previous 
   writes within the program.
3. __Repeat__: Loop until the program is reduced to a single literal. Because all expressions with 
   literal arguments return a literal result, all programs will eventually reduce to a literal.
4. __Commit__: Cas all keys in the local buffer conditioned on all revisions in the local snapshot. 
   The transactional guarantees of cas imply that program execution is __serializable__. 
   Serializability means that concurrent execution has the same effect as some sequential execution, 
   and, therefore, that program execution will be robust against race conditions.
   
## Optimizations
First, execution is tail-recursive. This allows programs to be composed of arbitrarily many
nested expressions without overflowing the stack frame during execution. It also allows the Scala
compiler to aggressively optimize execution into a tight loop.

Second, the runtime batches I/O. Reads are performed simultaneously whenever possible and writes
are buffered and simultaneously committed. By batching I/O, the runtime performs a minimal number
of operations on the database. This has significant performance implications, because I/O overhead
is overwhelmingly the bottleneck by many orders of magnitude in most programs.

[1]: https://en.wikipedia.org/wiki/Multiversion_concurrency_control
[2]: http://www.cl.cam.ac.uk/research/srg/netos/papers/2002-casn.pdf