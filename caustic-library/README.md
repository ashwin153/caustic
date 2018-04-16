# Standard Library
The runtime provides native support for an extremely limited subset of the operations that
programmers typically rely on to write programs. The standard library supplements the
functionality of the runtime by exposing a rich Scala DSL complete with static types, records,
math, collections, and control flow.

## Typing
The runtime natively supports just four dynamic types: ```flag```, ```real```, ```text```, and 
```null```. Dynamic versus static typing is a religious debate among programmers. Advocates of 
dynamic typing often mistakenly believe that type inference and coercive subtyping cannot be 
provided by a static type system. In fact, they can. Because static type systems are able to
detect type inaccuracies at compile-time, they allow programmers to write more concise and
correct code. The standard library provides rich static types and features
aggressive type inference and subtype polymorphism.

The standard library supports four ```Primitive``` types. In ascending order of precedence,
they are ```Boolean```, ```Int```, ```Double```, and ```String```. A ```Value```
represents a value of primitive type. There are two kinds of values. A ```Constant``` is an
immutable value, and a ```Variable``` is a mutable value. Variables may be stored locally in
memory or remotely in a database.

```scala
// Creates an integer local variable named x.
val x = Variable.Local[Int]("x")
// Creates a floating point remote variable named y.
val y = Variable.Remote[Double]("y")
// Assigns y to the sum of x and y.
y := x + y
// Assigns x to the product of x and 4.
x *= 4
// Does not compile, because y is not an integer.
x := y
// Does compile, because floor(y) is an integer.
x := floor(y)
```

## Records
In addition to these primitive types, the standard library also allows a ```Reference``` to be made
to a user-defined type. References use [Shapeless][1] to materialize compiler macros that permit the 
fields of an object to be statically manipulated and iterated. A current limitation is that objects 
cannot be self-referential; an object cannot have a field of its own type.

```scala
// An example type declaration.
case class Bar(
  a: String,
)

case class Foo(
  b: Int,
  c: Reference[Bar],
  d: Bar
)

// Constructs a remote reference to a Foo.
val x = Reference[Foo](Variable.Remote("x"))
// Returns the value of the field b.
x.get(@<'b>@)
// Does not compile, because z is not a field of Foo.
x.get(@<'z>@)
// Serializes x to a JSON string.
x.asJson
// Deletes all fields of x and all references.
x.delete(recursive = true)
// Constructs a local reference to a Foo.
val y = Reference[Foo](Variable.Local("y"))
// Copies x to y.
y := x
```

## Math
The runtime natively supports just nine mathematical operations: ```add```, ```sub```, ```mul```, 
```div```, ```pow```, ```log```, ```floor```, ```sin```, and ```cos```. However, these primitive 
operations are sufficient to derive the entire Scala [math][2] library using various mathematical 
identities and approximations. The ```div```, ```log```, ```sin```, and ```cos``` functions can 
actually be implemented in terms of the other primitive operations; however, native support for them 
was included in the runtime to improve performance. The standard library provides implementations 
for all functions enumerated in the following table.

| Function                 | Description                                                           |
|:-------------------------|:----------------------------------------------------------------------|
| ```abs(x)```             | Absolute value of ```x```.                                            | 
| ```acos(x)```            | Cosine inverse of ```x```.                                            |
| ```acot(x)```            | Cotangent inverse of ```x```.                                         | 
| ```acsc(x)```            | Consecant inverse of ```x```.                                         | 
| ```asec(x)```            | Secant inverse of ```x```.                                            | 
| ```asin(x)```            | Sine inverse of ```x```.                                              | 
| ```atan(x)```            | Tangent inverse of ```x```.                                           | 
| ```ceil(x)```            | Smallest integer greater than or equal to ```x```.                    |
| ```cos(x)```             | Cosine of ```x```.                                                    | 
| ```cosh(x)```            | Hyperbolic cosine of ```x```.                                         | 
| ```cot(x)```             | Cotangent of ```x```.                                                 |
| ```coth(x)```            | Hyperbolic cotangent of ```x```.                                      |
| ```csc(x)```             | Cosecant of ```x```.                                                  |
| ```csch(x)```            | Hyperbolic cosecant of ```x```.                                       |
| ```exp(x)```             | Exponential of ```x```.                                               |
| ```floor(x)```           | Largest integer less than or equal to ```x```.                        |
| ```log(x)```             | Natural logarithm of ```x```.                                         | 
| ```log(x, y)```          | Log base ```y``` of ```x```.                                          |
| ```log10(x)```           | Log base 10 of ```x```.                                               |
| ```log2(x)```            | Log base 2 of ```x```.                                                |
| ```pow(x, y)```          | Power of ```x``` to the ```y```.                                      |
| ```random()```           | Uniformly random number on [0, 1).                                    |
| ```round(x)```           | Closest integer to ```x```.                                           |
| ```round(x, y)```        | Closest multiple of ```y``` to ```x```.                               |
| ```sec(x)```             | Secant of ```x```.                                                    |
| ```sech(x)```            | Hyperbolic secant of ```x```.                                         |
| ```signum(x)```          | Returns the sign of ```x```.                                          |
| ```sin(x)```             | Sine of ```x```.                                                      | 
| ```sinh(x)```            | Hyperbolic sine of ```x```.                                           |
| ```sqrt(x)```            | Square root of ```x```.                                               | 
| ```tan(x)```             | Tangent of ```x```.                                                   |

## Collections
The runtime has no native support for collections of key-value pairs. The standard library
provides implementations of three fundamental data structures: ```List```, ```Set```, and ```Map```. 
These collections are mutable and statically-typed. Collections take care of the messy details of 
mapping structured data onto a flat namespace, and feature prefetched iteration. A current 
limitation is that collections may only contain primitive types.

```scala
// Constructs a map from string to boolean.
val x = Map[String, Boolean](Variable.Remote("y"))
// Puts an entry in the map.
x += "foo" -> true
// Serializes x to a JSON string.
x.asJson
// Constructs a list of integers.
val x = List[Int](Variable.Local("x"))
// Increments each element in the list.
x.foreach(_ + 1)
```

## Control Flow
The runtime natively supports control flow operations like ```branch```, ```cons```, and 
```repeat```. However, these operations are syntactically challenging to express. The standard 
library uses structural typing to provide support for ```If```, ```While```, ```Return```, 
```Assert```, and ```Rollback```. The standard library uses an implicitly available parsing 
```Context``` to track modifications made to variables, references, and collections and to detect
when any control flow operations are called.

```scala
 // If statements.
If (x < 3) {
  x += 3
}

// If/Else statements
If (y < x) {
  y += 1
} Else {
  x += 1
}

// Ternary operator.
val z = If (y === x) { y - 1 } Else { y + 1 }

// While loops.
While (x <> y) {
  x *= 2
}
```  

[1]: https://github.com/milessabin/shapeless
[2]: https://www.scala-lang.org/api/2.12.1/scala/math/index.html
