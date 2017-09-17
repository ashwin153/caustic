# Syntax
The ```caustic-runtime``` enables operations to be transactionally applied to any key-value store. Why is this useful? Inside of every computer is the simplest example of a key-value store - it's called memory. Memory maps each number, or address, to the value stored at the location. Despite this primitive storage system, a computer is capable of running extraordinarily complex programming languages complete with features like objects, variables, functions, and types. How is this possible? Eventually these high-level programming languages compile into a sequence of operations on memory addresses.

However, the language exposed by the runtime is verbose and primitive. As transactions become larger in size and broader in scope, it becomes more challenging to write correct and maintainable programs in this manner. The ```caustic-runtim``` pacakge defines the ```Caustic``` programming language, that compiles into executable code on the ```caustic-runtime```. Therefore, programs written in ```Caustic``` may be transactionally run on *any* key-value store. This makes it incredibly easy to perform distributed computation. Say goodbye to locks, semaphores, synchronization, and race conditions. Stop worrying about thread-safety. Let ```Caustic``` take care of that for you.

## Grammar
The grammar of the language is very similar to that of Scala and [Thrift][3]. In fact, the grammar is so similar that the compiler is able to rewrite programs as Scala implementations of Thrift interfaces. Therefore, programs are interoperable with any Thrift-compatible language and all existing JVM infrastructure. For example, consider the following distributed counter in Caustic. Because Caustic is entirely transactional, no synchronization mechanisms (locks, semaphores, etc.) are required to guarantee that the counter is distributable. __Concurrency is automatic!__ 

```
module caustic.example

/**
 * A total quantity.
 * 
 * @param value Current total.
 */
record Total {
  value: Integer
} 

/**
 * A distributed counting service.
 */
service Counter {
  
  /**
   * Increments the total and returns the current value.
   * 
   * @param x Total reference.
   * @return Current value.
   */
  def increment(x: Total): Integer = {
    if (x.exists) {
      x.value += 1
    } else {
      x.value = 1
    } 
  }

} 
```

## Compiler
The compiler is a recursive descent parser that generates executable Scala implementations of Thrift interfaces.

- __Records__ are compiled into Thrift struct interfaces.
- __Services__ are compiled into Thrift service interfaces and into an executable, Scala server that implements the interface. 
- __Blocks__ are assigned a unique namespace by concatenating the parent block's namespace and a block identifier. All local variable names within a block are prefixed by the namespace in order to implement lexical scoping.
- __Expressions__ are compiled into operations on the ```Codec``` which is a Scala library that is included and imported in the generated sources that builds runtime-compatible, Thrift transactions.