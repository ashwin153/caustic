package caustic.library

import caustic.library.typing._
import caustic.runtime
import caustic.runtime._

package object math {

  // Scalars.
  val Zero                                      : Value[Int]    = 0
  val Half                                      : Value[Double] = 0.5
  val One                                       : Value[Int]    = 1
  val Two                                       : Value[Int]    = 2
  val Ten                                       : Value[Int]    = 10
  val Pi                                        : Value[Double] = scala.math.Pi
  val E                                         : Value[Double] = scala.math.E

  // Functions.
  def abs   [X <: Double](x: Value[X])          : Value[X]      = branch(x < Zero, -x, x)
  def acos  (x: Value[Double])                  : Value[Double] = Pi / 2 - asin(x)
  def acot  (x: Value[Double])                  : Value[Double] = Pi / 2 - atan(x)
  def acsc  (x: Value[Double])                  : Value[Double] = Pi / 2 - asec(x)
  def asec  (x: Value[Double])                  : Value[Double] = acos(pow(x, -1))
  def asin  (x: Value[Double])                  : Value[Double] = x + pow(x, 3) / 6 + pow(x, 5) * 3 / 40 + pow(x, 7) * 15 / 336
  def atan  (x: Value[Double])                  : Value[Double] = asin(x / pow(x * x + 1, Half))
  def ceil  (x: Value[Double])                  : Value[Int]    = floor(x) + One
  def cos   (x: Value[Double])                  : Value[Double] = runtime.cos(x)
  def cosh  (x: Value[Double])                  : Value[Double] = (exp(x) + exp(-x)) / Two
  def cot   (x: Value[Double])                  : Value[Double] = cos(x) / sin(x)
  def coth  (x: Value[Double])                  : Value[Double] = cosh(x) / sinh(x)
  def csc   (x: Value[Double])                  : Value[Double] = One / sin(x)
  def csch  (x: Value[Double])                  : Value[Double] = One / sinh(x)
  def exp   (x: Value[Double])                  : Value[Double] = pow(E, x)
  def floor (x: Value[Double])                  : Value[Int]    = runtime.floor(x)
  def log   (x: Value[Double])                  : Value[Double] = runtime.log(x)
  def log   (x: Value[Double], y: Value[Double]): Value[Double] = log(x) / log(y)
  def log10 (x: Value[Double])                  : Value[Double] = log(x, Ten)
  def log2  (x: Value[Double])                  : Value[Double] = log(x, Two)
  def pow   (x: Value[Double], y: Value[Double]): Value[Double] = runtime.pow(x, y)
  def random()                                  : Value[Double] = runtime.random()
  def round (x: Value[Double])                  : Value[Int]    = branch(x - floor(x) < Half, floor(x), ceil(x))
  def round (x: Value[Double], y: Value[Double]): Value[Double] = branch(x % y < y / 2, x - x % y, x - x % y + y)
  def sec   (x: Value[Double])                  : Value[Double] = One / cos(x)
  def sech  (x: Value[Double])                  : Value[Double] = One / cosh(x)
  def signum(x: Value[Double])                  : Value[Int]    = branch(x === 0, 0, branch(x < 0, -1, 1))
  def sin   (x: Value[Double])                  : Value[Double] = runtime.sin(x)
  def sinh  (x: Value[Double])                  : Value[Double] = (exp(x) - exp(-x)) / Two
  def sqrt  (x: Value[Double])                  : Value[Double] = pow(x, Half)
  def tan   (x: Value[Double])                  : Value[Double] = sin(x) / cos(x)
  def tanh  (x: Value[Double])                  : Value[Double] = sinh(x) / cosh(x)

}