package caustic.lang

import caustic.runtime.typing._
import caustic.runtime

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
  def abs   [X <: Double](x: Value[X])          : Value[X]      = runtime.branch(x < Zero, -x, x)
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
  def pow   (x: Value[Double], y: Value[Double]): Value[Double] = runtime.pow(x, y)
  def random()                                  : Value[Double] = runtime.random()
  def round (x: Value[Double])                  : Value[Int]    = runtime.branch(x - floor(x) < Half, floor(x), ceil(x))
  def sec   (x: Value[Double])                  : Value[Double] = One / cos(x)
  def sech  (x: Value[Double])                  : Value[Double] = One / cosh(x)
  def sin   (x: Value[Double])                  : Value[Double] = runtime.sin(x)
  def sinh  (x: Value[Double])                  : Value[Double] = (exp(x) - exp(-x)) / Two
  def sqrt  (x: Value[Double])                  : Value[Double] = pow(x, Half)
  def tan   (x: Value[Double])                  : Value[Double] = sin(x) / cos(x)
  def tanh  (x: Value[Double])                  : Value[Double] = sinh(x) / cosh(x)

}