# Programs
The runtime is a virtual machine that dynamically compiles __programs__ into __transactions__ that 
are *atomically* and *consistently* executed on any transactional key-value store in *isolation*.
Programs are abstract-syntax trees that are composed of scalar __literal__ values and 
__expressions__ that transform literal arguments into literal results. The runtime uses 
tail-recursive, partial evaluation to gradually reduce programs to a single literal result.

Literals may be of type ```Flag```, ```Real```, ```Text```, and ```Null``` which correspond to
bool, double, string, and null respectively in most C-style languages. The following table
enumerates the various expressions supported by the runtime. The list of expressions may seem 
primitive; however, they may be composed together arbitrarily to create more sophisticated 
subroutines.

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

# Execution
Changes to keys are detected using [multiversion concurrency control][1]. Each key is associated 
with a __revision__, or versioned value, whose version number is incremented each time that its 
value is modified. A revision ```A``` *conflicts with* ```B``` if ```A``` and ```B``` correspond 
to the same key, and the version of ```A``` is greater than ```B```. Conflict is an 
[asymmetric relation][2]; if ```A``` conflicts with ```B```, then ```B``` does not conflict with 
```A```. 

The runtime can *execute* programs on any key-value store that supports *get* and *cas*. A get 
retrieves the revisions of a set of keys and a cas conditionally updates a set of keys if and 
only if a set of dependent versions do not conflict. Given correct implementations of get and cas, 
the runtime executes programs according to the following procedure.

1. __Retrieve__: Call get on all keys that are read and written by the transaction, and add the 
   returned revisions to a local snapshot. In order to guarantee [snapshot isolation][3], get is 
   only called when a key is read or written for the first time. By batching reads and writes 
   and avoiding duplicate fetches, databases are guaranteed to perform a minimal number of 
   roundtrips to and from the underlying key-value store. 
2. __Evaluate__: Recursively replace all expressions that have literal operands with their 
   corresponding literal result. For example, ```add(real(1), sub(real(0), real(2)))``` returns 
   ```real(-1)```. The result of all ```write```expressions is saved to a local buffer and the 
   result of all ```read``` expressions is the latest value of the key in either the local buffer or 
   snapshot.  
3. __Repeat__: Re-evaluate the program until it reduces to a single literal value. Because all 
   expressions with literal operands return a literal value, all programs eventually reduce to a 
   literal.
4. __Commit__: Call cas on all keys that are written by the program conditioned on all
   the keys that are read or written by the program remaining unchanged. Because programs are 
   executed with snapshot isolation, they are guaranteed to be serializable. Serializability 
   implies that concurrent execution has the same effect as sequential execution, and, therefore,
   that program execution will be robust against race conditions.
   
# Optimizations
First, the runtime is tail-recursive. Therefore, programs may be composed of arbitrarily many nested
expressions. Second, the runtime batches I/O. Reads are performed simultaneously whenever possible 
and writes are buffered. This significantly reduces amount of I/O performed during execution.

[1]: https://en.wikipedia.org/wiki/Multiversion_concurrency_control
[2]: https://en.wikipedia.org/wiki/Asymmetric_relation
[3]: https://en.wikipedia.org/wiki/Snapshot_isolation