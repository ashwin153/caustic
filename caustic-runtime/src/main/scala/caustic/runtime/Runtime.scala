package caustic.runtime

import caustic.runtime.Literal._
import caustic.runtime.Retry._
import caustic.runtime.Runtime._
import caustic.cluster.Cluster
import caustic.cluster.protocol.Beaker

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * A transactional virtual machine.
 *
 * @param cluster Beaker [[Cluster]].
 */
class Runtime(cluster: Cluster[Beaker.Client]) {

  /**
   * Executes the [[Program]] asynchronously and returns the result, retrying failures with backoff.
   *
   * @param backoffs  Backoff durations.
   * @param program [[Program]] to execute.
   * @throws Rollbacked If the [[Program]] was rolled back.
   * @throws Aborted If the [[Program]] could not be executed.
   * @throws Fault If the [[Program]] is illegally constructed.
   * @return [[Literal]] result or an exception on failure.
   */
  def execute(backoffs: Seq[FiniteDuration])(program: Program)(
    implicit ec: ExecutionContext
  ): Future[Literal] = {
    attempt(backoffs)(execute(program))
  }

  /**
   * Executes the [[Program]] and returns the result. Programs are repeatedly partially evaluated
   * until they are reduced to a single [[Literal]] value. Automatically batches reads and buffers
   * writes.
   *
   * @param program [[Program]] to execute.
 *
   * @throws Rollbacked If the [[Program]] was rolled back.
   * @throws Aborted If the [[Program]] could not be executed.
   * @throws Fault If the [[Program]] is illegally constructed.
   * @return [[Literal]] result or exception on failure.
   */
  def execute(program: Program): Try[Literal] = {
    val depends  = mutable.Map.empty[Key, Version]
    val snapshot = mutable.Map.empty[Key, Literal]
    val buffer   = mutable.Map.empty[Key, Literal]
    val locals   = mutable.Map.empty[Key, Literal]

    def evaluate(iteration: Program): Try[Literal] = {
      // Fetch all keys that are read (for their value) and written (for their version), that have
      // not been read before (to avoid changes in value and version) to ensure that the evaluation
      // of an expression is correct and consistent.
      @tailrec
      def rwset(stack: List[Program], aggregator: Set[Key]): Set[Key] = stack match {
        case Nil => aggregator
        case (_: Literal) :: rest => rwset(rest, aggregator)
        case Expression(Read, Text(key) :: _) :: rest => rwset(rest, aggregator + key)
        case Expression(Write, Text(key) :: _) :: rest => rwset(rest, aggregator + key)
        case (o: Expression) :: rest => rwset(o.operands ::: rest, aggregator)
      }

      val keys  = rwset(List(iteration), Set.empty) -- depends.keys
      depends ++= keys.map(_ -> 0L)

      // Fetch the keys, update the local snapshot, and reduce the program. If the result is a
      // literal then return, otherwise recurse on the partially evaluated program.
      this.cluster.random(_.get(keys)) flatMap { r =>
        depends  ++= r.mapValues(r => r.version)
        snapshot ++= r.mapValues(r => deserialize(r.value))

        reduce(List(iteration), List.empty) match {
          case l: Literal => Success(l)
          case o: Expression => evaluate(o)
        }
      }
    }

    @tailrec
    def reduce(stack: List[Any], results: List[Program]): Program = (stack, results) match {
      // Return Results.
      case (Nil, _) =>
        if (results.size != 1)
          throw Fault(s"Transaction evaluates to $results.")
        else
          results.head

      // Replace Literals.
      case ((l: Literal) :: rest, rem) => reduce(rest, l :: rem)

      // Expand Expressions.
      case (Expression(Read, Text(k) :: Nil) :: rest, rem) =>
        reduce(rest, buffer.getOrElse(k, snapshot(k)) :: rem)
      case (Expression(Write, Text(k) :: (v: Literal) :: Nil) :: rest, rem) =>
        buffer += k -> v
        reduce(rest, v :: rem)
      case (Expression(Branch, cmp :: pass :: fail :: Nil) :: rest, rem) =>
        reduce(cmp :: Branch :: rest, pass :: fail :: rem)
      case (Expression(Cons, first :: second :: Nil) :: rest, rem) =>
        reduce(first :: Cons :: rest, second :: rem)
      case (Expression(Repeat, c :: b :: Nil) :: rest, rem) =>
        reduce(branch(c, cons(b, repeat(c, b)), Null) :: rest, rem)
      case (Expression(Random, Nil) :: rest, rem) =>
        reduce(rest, real(scala.util.Random.nextDouble()) :: rem)
      case ((e: Expression) :: rest, rem) =>
        reduce(e.operands.reverse ::: e.operator :: rest, rem)

      // Simplify Core Expressions.
      case (Read :: rest, k :: rem) => reduce(rest, read(k) :: rem)
      case (Write :: rest, k :: v :: rem) => reduce(rest, write(k, v) :: rem)
      case (Load :: rest, Text(k) :: rem) => reduce(rest, locals.getOrElse(k, Null) :: rem)
      case (Load :: rest, k :: rem) => reduce(rest, load(k) :: rem)
      case (Store :: rest, Text(k) :: (v: Literal) :: rem) => locals += k -> v; reduce(rest, v :: rem)
      case (Store :: rest, k :: v :: rem) => reduce(rest, store(k, v) :: rem)
      case (Rollback :: _, (l: Literal) :: _) => throw Rollbacked(l)
      case (Rollback :: rest, x :: rem) => reduce(rest, rollback(x) :: rem)
      case (Repeat :: rest, Flag(false) :: _ :: rem) => reduce(rest, rem)
      case (Repeat :: rest, c :: b :: rem) => reduce(rest, repeat(c, b) :: rem)
      case (Cons :: rest, f :: s :: rem) if f.isInstanceOf[Literal] => reduce(s :: rest, rem)
      case (Cons :: rest, f :: s :: rem) => reduce(rest, cons(f, s) :: rem)
      case (Branch :: rest, Flag(true) :: pass :: _ :: rem) => reduce(pass :: rest, rem)
      case (Branch :: rest, Flag(false) :: _ :: fail :: rem) => reduce(fail :: rest, rem)
      case (Branch :: rest, Null :: _ :: fail :: rem) => reduce(fail :: rest, rem)
      case (Branch :: rest, (_: Literal) :: pass :: _ :: rem) => reduce(pass :: rest, rem)
      case (Branch :: rest, c :: p :: f :: rem) => reduce(rest, branch(c, p, f) :: rem)
      case (Random :: rest, rem) => reduce(rest, random() :: rem)

      // Simplify String Expressions.
      case (Length :: rest, x :: rem) => reduce(rest, length(x) :: rem)
      case (Matches :: rest, x :: y :: rem) => reduce(rest, matches(x, y) :: rem)
      case (Contains :: rest, x :: y :: rem) => reduce(rest, contains(x, y) :: rem)
      case (Slice :: rest, x :: l :: h :: rem) => reduce(rest, slice(x, l, h) :: rem)
      case (IndexOf :: rest, x :: y :: rem) => reduce(rest, indexOf(x, y) :: rem)

      // Simplify Math Expressions.
      case (Add :: rest, x :: y :: rem) => reduce(rest, add(x, y) :: rem)
      case (Sub :: rest, x :: y :: rem) => reduce(rest, sub(x, y) :: rem)
      case (Mul :: rest, x :: y :: rem) => reduce(rest, mul(x, y) :: rem)
      case (Div :: rest, x :: y :: rem) => reduce(rest, div(x, y) :: rem)
      case (Mod :: rest, x :: y :: rem) => reduce(rest, mod(x, y) :: rem)
      case (Pow :: rest, x :: y :: rem) => reduce(rest, pow(x, y) :: rem)
      case (Log :: rest, x :: rem) => reduce(rest, log(x) :: rem)
      case (Sin :: rest, x :: rem) => reduce(rest, sin(x) :: rem)
      case (Cos :: rest, x :: rem) => reduce(rest, cos(x) :: rem)
      case (Floor :: rest, x :: rem) => reduce(rest, floor(x) :: rem)

      // Simplify Logical Expressions.
      case (Both :: rest, x :: y :: rem) => reduce(rest, both(x, y) :: rem)
      case (Either :: rest, x :: y :: rem) => reduce(rest, either(x, y) :: rem)
      case (Negate :: rest, x :: rem) => reduce(rest, negate(x) :: rem)

      // Simplify Comparison Expressions.
      case (Equal :: rest, x :: y :: rem) => reduce(rest, equal(x, y) :: rem)
      case (Less :: rest, x :: y :: rem) => reduce(rest, less(x, y) :: rem)

      // Default Error.
      case _ => throw Fault(s"$stack cannot be applied to $results.")
    }

    // Recursively reduce the program, and then conditionally persist all changes made by the
    // transaction to the underlying database if and only if the versions of its various
    // dependencies have not changed. Filter out empty first changes to allow local variables.
    evaluate(program) recoverWith { case e: Rollbacked =>
      this.cluster.random(_.cas(depends.toMap, Map.empty)) match {
        case Success(true) => Failure(e)
        case _ => Failure(Aborted)
      }
    } flatMap { r =>
      this.cluster.random(_.cas(depends.toMap, buffer.mapValues(serialize).toMap)) match {
        case Success(true) => Success(r)
        case _ => Failure(Aborted)
      }
    }
  }

}

object Runtime {

  /**
   * A non-retryable failure that terminates execution and discards all writes.
   *
   * @param result [[Literal]] return value.
   */
  case class Rollbacked(result: Literal) extends Exception with NonRetryable

  /**
   * A retryable failure that indicates a [[Program]] could not be executed.
   */
  case object Aborted extends Exception

  /**
   * An non-retryable failure that is thrown when a [[Program]] is illegally constructed.
   *
   * @param message Error message.
   */
  case class Fault(message: String) extends Exception with NonRetryable

  /**
   * Constructs a [[Runtime]] connected to the [[Cluster]].
   *
   * @param config [[Cluster]] configuration.
   * @return Connected [[Runtime]].
   */
  def apply(config: Cluster.Config): Runtime = new Runtime(Cluster(Beaker.Service, config))

}