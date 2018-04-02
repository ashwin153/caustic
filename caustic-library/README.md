# Typing
The standard library supports the following primitive types in descending order of precedence:
```Null```, ```String```, ```Double```, ```Int```, and ```Boolean```. A __value__ is a primitive
scalar that may be manipulated using familiar comparison and logical operators like ```<=```, and 
```===```. Additional numeric and textual operations like ```+``` and ```substring``` are supported
for values of the corresponding types. There are two kinds of values: __constants__ and 
__variables__. Constants store an immutable value and variables store a mutable value either locally 
in a buffer or remotely in the database.

# Math
In addition to the standard arithmetic operations on numeric values, the standard library also
provides a rich math package. The following table enumerates the various supported functions.

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
| ```sin(x)```             | Sine of ```x```.                                                      | 
| ```sinh(x)```            | Hyperbolic sine of ```x```.                                           |
| ```sqrt(x)```            | Square root of ```x```.                                               | 
| ```tan(x)```             | Tangent of ```x```.                                                   |

# Records
The standard library supports __references__ to records. It uses [Shapeless][1] to reflect on 
standard Scala case classes and enable transactional accesses and modifications to their fields. 
Fields may be primitives, nested records, or references to other records. For example, the following 
case class definition is compatible with the standard library.

```scala
case class Foo(
  x: Int,
  y: Foo,
  z: Reference[Foo]
)
```

# Control Flow
By themselves, values and their corresponding operations serve little purpose. It is only through
compositions of these operations that non-trivial programs can be formed. The standard library uses
an implicitly available __context__ to track the various operations performed on a value and compose
them together into a single program. It uses [structural types][2] to provide convenient control 
flow operations like conditional branching and loops whenever an implicit context is in scope. For 
example, consider the following subroutine.

```scala
def example(x: Value[Int])(implicit context: Context): Unit = {
  // If.
  If (x == 0) {
    x += 1
  }
  
  // If-Else.
  If (x > 0) {
    x -= 1
  } Else {
    x += 2
  }
  
  // While.
  While (x < 0) {
    x += 1
  }
  
  // Exceptions.
  Rollback (x)
}
```

[1]: https://github.com/milessabin/shapeless
[2]: https://twitter.github.io/scala_school/advanced-types.html#structural
