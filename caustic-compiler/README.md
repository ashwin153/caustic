# Compiler
The ```caustic-compiler``` compiles the Caustic programming language.

## Language
The Caustic language is strong typed, thread-safe, and interoperable. It features aggressive
type-inference and a [terse grammar][2]. The following program is an example of a distributed 
counter written in Caustic. This program can be executed without modification on any underlying 
key-value store, and run simulatenously without error. The program compiles into a Scala library,
that is compatible with all Scala frameworks and tooling.

```
module caustic.example

/**
 * A counter.
 *
 * @param value Current value.
 */
record Total {
  value: Int
}

/**
 * A distributed counting service.
 */
service Counter {

  /**
   * Increments the total and returns its current value.
   *
   * @param x Total pointer.
   * @return Current value.
   */
  def increment(x: Total&): Int = {
    if x.value {
      x.value += 1
    } else {
      x.value = 1
    }
  }

}
```

## Compilation
The Caustic compiler, or ```cc```, is modeled after the Rust compiler's [query system][1]. The
execution of a goal like ```repl```, may trigger other goals like ```compile``` and ```run```
to be executed first. This will enable memoization and fast incremental compilation, which should
significantly improve the performance of the compiler. The following queries are currently supported 
by the compiler, and are executed by running ```cc <query> <file>``` from the command line.

- ```declare```: Parses all the declarations in a program. Requires ```simplify```.
- ```generate```: Code generation. Requires ```declare```.
- ```simplify```: Simplifies an expression or block of expressions.

[1]: https://github.com/rust-lang/rust/tree/master/src/librustc/ty/maps
[2]: https://github.com/ashwin153/caustic/blob/master/caustic-compiler/src/main/antlr/Caustic.g4