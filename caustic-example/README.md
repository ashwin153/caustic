# Compiler
The ```caustic-compiler``` compiles the Caustic programming language.

## Getting Started
The Caustic compiler, or ```CC```, is modeled after the Rust compiler's [query system][1]. The
execution of a query like ```repl```, may trigger other queries like ```compile``` and ```run```
to be executed first. Query execution is memoized to avoid duplication of work. The following
queries are supported by the compiler, and are executed by running ```cc <query> <file>``` from the
command line.

- ```check```: Runs the type checker.
- ```compile```: Compiles a programs. Requires ```check``` and ```gen```.
- ```gen```: Code generation. Requires ```check```.
- ```repl```: Opens a REPL session. Requires ```run```.
- ```run```: Executes a program. Requires ```compile```.

[1]: https://github.com/rust-lang/rust/tree/master/src/librustc/ty/maps