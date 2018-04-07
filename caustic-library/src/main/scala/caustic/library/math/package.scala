package caustic.library

import caustic.library.typing._
import caustic.library.typing.Value._
import caustic.runtime
import caustic.runtime._

package object math {

  val E: Value[Double] = scala.math.E
  val Pi: Value[Double] = scala.math.Pi

  /**
   * Returns the absolute value of x.
   *
   * @param x (-∞, ∞).
   * @return [0, ∞).
   */
  def abs[X >: Int <: Double](x: Value[X]): Value[X] = branch(x < 0, -x, x)

  /**
   * Returns the approximate cosine inverse of x.
   *
   * @param x [-1, 1].
   * @return [0, π].
   */
  def acos(x: Value[Double]): Value[Double] = Pi / 2 - asin(x)

  /**
   * Returns the approximate cotangent inverse of x.
   *
   * @param x (-∞, ∞).
   * @return (0, π).
   */
  def acot(x: Value[Double]): Value[Double] = Pi / 2 - atan(x)

  /**
   * Returns the approximate cosecant inverse of x.
   *
   * @param x (-∞, -1] U [1, ∞).
   * @return (-π, -π/2] U (0, π/2]
   */
  def acsc(x: Value[Double]): Value[Double] = Pi / 2 - asec(x)

  /**
   * Returns the approximate secant inverse of x.
   *
   * @param x (-∞, -1] U [1, ∞).
   * @return (0, π/2) U [π, 3π/2).
   */
  def asec(x: Value[Double]): Value[Double] = acos(1 / x)

  /**
   * Returns the approximate sine inverse of x.
   *
   * @param x [-1, 1].
   * @return [-π/2, π/2].
   */
  def asin(x: Value[Double]): Value[Double] =
    x + pow(x, 3) / 6.0 + pow(x, 5) * 3 / 40.0 + pow(x, 7) * 15 / 336.0

  /**
   * Returns the approximate tangent inverse of x.
   *
   * @param x (-∞, ∞).
   * @return (-π/2, π/2).
   */
  def atan(x: Value[Double]): Value[Double] = asin(x / sqrt(x * x + 1))

  /**
   * Returns the cube root of x.
   *
   * @param x (-∞, ∞).
   * @return (-∞, ∞).
   */
  def cbrt(x: Value[Double]): Value[Double] = pow(x, 1 / 3.0)

  /**
   * Returns the smallest integer greater than or equal to x.
   *
   * @param x (-∞, ∞).
   * @return (-∞, ∞).
   */
  def ceil(x: Value[Double]): Value[Int] = floor(x) + 1

  /**
   * Returns the cosine of x.
   *
   * @param x (-∞, ∞).
   * @return [-1, 1].
   */
  def cos(x: Value[Double]): Value[Double] = runtime.cos(x)

  /**
   * Returns the hyperbolic cosine of x.
   *
   * @param x (-∞, ∞).
   * @return [1, ∞).
   */
  def cosh(x: Value[Double]): Value[Double] = (exp(x) + exp(-x)) / 2

  /**
   * Returns the cotangent of x.
   *
   * @param x (-∞, ∞) / nπ.
   * @return (-∞, ∞).
   */
  def cot(x: Value[Double]): Value[Double] = cos(x) / sin(x)

  /**
   * Returns the hyperbolic cotangent of x.
   *
   * @param x (-∞, 0) U (0, ∞).
   * @return (-∞, -1) U (1, ∞).
   */
  def coth(x: Value[Double]): Value[Double] = cosh(x) / sinh(x)

  /**
   * Returns the cosecant of x.
   *
   * @param x (-∞, ∞) / nπ.
   * @return (-∞, -1] U [1, ∞).
   */
  def csc(x: Value[Double]): Value[Double] = pow(sin(x), -1)

  /**
   * Returns the hyperbolic cosecant of x.
   *
   * @param x (-∞, 0) U (0, ∞).
   * @return (-∞, 0) U (0, ∞).
   */
  def csch(x: Value[Double]): Value[Double] = pow(sinh(x), -1)

  /**
   * Returns the exponential of x.
   *
   * @param x (-∞, ∞).
   * @return (0, ∞).
   */
  def exp(x: Value[Double]): Value[Double] = pow(E, x)

  /**
   * Returns the exponential of x - 1. Equivalent to exp(x) - 1.
   *
   * @param x (-∞, ∞).
   * @return (-1, ∞).
   */
  def expm1(x: Value[Double]): Value[Double] = exp(x) - 1

  /**
   * Returns the largest integer less than or equal to x.
   *
   * @param x (-∞, ∞).
   * @return (-∞, ∞).
   */
  def floor(x: Value[Double]): Value[Int] = runtime.floor(x)

  /**
   * Returns the hypotenuse of the triangle with base x and height y.
   *
   * @param x (-∞, ∞).
   * @param y (-∞, ∞).
   * @return (-∞, ∞).
   */
  def hypot(x: Value[Double], y: Value[Double]): Value[Double] = sqrt(x * x + y * y)

  /**
   * Returns the natural logarithm of x.
   *
   * @param x (0, ∞).
   * @return (-∞, ∞).
   */
  def log(x: Value[Double]): Value[Double] = runtime.log(x)

  /**
   * Returns the logarithm base 10 of x.
   *
   * @param x (0, ∞).
   * @return (-∞, ∞).
   */
  def log10(x: Value[Double]): Value[Double] = log(x) / log(10)

  /**
   * Returns the natural logarithm of (x + 1). Equivalent to log(x + 1).
   *
   * @param x (-1, ∞).
   * @return (-∞, ∞).
   */
  def log1p(x: Value[Double]): Value[Double] = log(x + 1)

  /**
   * Returns x raised to the power y.
   *
   * @param x (-∞, ∞).
   * @param y (-∞, ∞) if x != 0 else [0, ∞).
   * @return (-∞, ∞).
   */
  def pow(x: Value[Double], y: Value[Double]): Value[Double] = runtime.pow(x, y)

  /**
   * Returns a uniformly random number on the interval [0, 1).
   *
   * @return [0, 1).
   */
  def random(): Value[Double] = scala.math.random()

  /**
   * Returns x rounded to the nearest integer, rounding to the nearest even number if equidistant.
   *
   * @param x (-∞, ∞).
   * @return (-∞, ∞).
   */
  def rint(x: Value[Double]): Value[Int] = branch(floor(x) % 2 === 0, x - floor(x) <= 0.5, round(x))

  /**
   * Returns x rounded to the nearest integer, rounding up if equidistant.
   *
   * @param x (-∞, ∞).
   * @return (-∞, ∞).
   */
  def round(x: Value[Double]): Value[Int] = branch(x - floor(x) < 0.5, floor(x), ceil(x))

  /**
   * Returns x rounded to the nearest multiple of y, rounding up if equidistant.
   *
   * @param x (-∞, ∞).
   * @param y (-∞, ∞).
   * @return (-∞, ∞).
   */
  def round(x: Value[Double], y: Value[Double]): Value[Double] =
    branch(x % y < y / 2, x - x % y, x - x % y + y)

  /**
   * Returns the secant of x.
   *
   * @param x (-∞, ∞) / (nπ - π/2).
   * @return (-∞, -1] U [1, ∞).
   */
  def sec(x: Value[Double]): Value[Double] = 1 / cos(x)

  /**
   * Returns the hyperbolic secant of x.
   *
   * @param x (-∞, ∞).
   * @return (0, 1).
   */
  def sech(x: Value[Double]): Value[Double] = 1 / cosh(x)

  /**
   * Returns the sign of x.
   *
   * @param x (-∞, ∞).
   * @return {-1, 0, 1}.
   */
  def signum(x: Value[Double]): Value[Int] = branch(x === 0, 0, branch(x < 0, -1, 1))

  /**
   * Returns the sine of x.
   *
   * @param x (-∞, ∞).
   * @return [-1, 1].
   */
  def sin(x: Value[Double]): Value[Double] = runtime.sin(x)

  /**
   * Returns the hyperbolic sine of x.
   *
   * @param x (-∞, ∞).
   * @return (-∞, ∞).
   */
  def sinh(x: Value[Double]): Value[Double] = (exp(x) - exp(-x)) / 2

  /**
   * Returns the square root of x.
   *
   * @param x [0, ∞).
   * @return [0, ∞).
   */
  def sqrt(x: Value[Double]): Value[Double] = pow(x, 0.5)

  /**
   * Returns the tangent of x.
   *
   * @param x (-∞, ∞) / (nπ - π/2).
   * @return (-∞, ∞).
   */
  def tan(x: Value[Double]): Value[Double] = sin(x) / cos(x)

  /**
   * Returns the hyperbolic tangent of x.
   *
   * @param x (-∞, ∞).
   * @return (-1, 1).
   */
  def tanh(x: Value[Double]): Value[Double] = sinh(x) / cosh(x)

}