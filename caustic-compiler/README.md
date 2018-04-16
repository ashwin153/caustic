# Compiler
The standard library provides additional functionality that is absent in the runtime to make it
easier to construct programs. However, the standard library does not address the syntactic
challenges of expressing programs. At times the standard library is forced to use unintuitive
operators like ```:=``` and ```<>``` and verbose declarations like ```Variable.Remote("x")```, 
because of the limitations of implementing a language within a language.

The compiler translates programs written in the statically-typed, object-oriented Caustic
programming language into operations on the standard library. The Caustic language is to the runtime
as C is to Assembly. While runtime programs are powerful, they lack the expressivity of most modern
programming languages. The Caustic compiler, or ```Causticc```, translates code written in Caustic
into runtime-compatible programs, to provide both the robustness of a general-purpose programming
language and the transactional guarantees of the runtime. For example, consider the following
example of a distributed counter written in Caustic. This program may be run without modification
on any underlying volume and distributed arbitrarily without error. The [Akka][1] project also
provides a similar [implementation][2] of a backend-agnostic distributed counter. Their 
implementation is almost seven times longer.

```
module caustic.example

/**
 * A count.
 *
 * @param value Current value.
 */
struct Total {
  value: Int
}

/**
 * A distributed counter.
 */
service Counter {

  /**
   * Increments the total and returns its current value.
   *
   * @param x Reference to total.
   * @return Current value.
   */
  def increment(x: Total&): Int = {
    if (x.value) x.value += 1 else x.value = 1
    x.value
  }

}
```

## Implementation
The compiler uses [ANTLR][3] to generate a predicated LL(*) parser from an ANTLR [grammar file][4]. 
It applies this parser to source files and walks the resulting parse-tree to generate code
compatible with the standard library. Most statements in Caustic have direct equivalents in the
standard library, and, for the most part, the compiler acts as a kind of intelligent 
find-and-replace. However, there are certain aspects of the compiler that are non-trivial.

First, the compiler performs type inference. It is able statically verify types and method
signatures by maintaining a lexically-scoped __universe__ of the various variables, records,
and functions that have been defined. Because the type system is relatively simplistic, static
types almost never need to be specified. The combination of a static type system and aggressive
type inference allows programs to be both type-safe and concise.

Second, the compiler is directly integrated in the [Pants][5] build system. Pants is an open-source, 
cross-language build system. Integration into Pants means that Caustic programs are interoperable
with a variety of existing languages and tooling.

Third, the compiler provides a [TextMate][6] bundle that implements syntax highlighting and code 
completion for most text editors and IDEs to make it easier for programmers to use the language.

[1]: https://akka.io/
[2]: https://git.io/vxS6u
[3]: http://www.antlr.org/
[4]: https://github.com/ashwin153/caustic/blob/master/caustic-compiler/src/main/antlr/Caustic.g4
[5]: https://www.pantsbuild.org/
[6]: https://macromates.com/