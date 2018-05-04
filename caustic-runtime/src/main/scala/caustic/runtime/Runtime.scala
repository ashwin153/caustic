package caustic.runtime

import beaker.client._
import caustic.runtime.Runtime._
import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

/**
 * A transactional virtual machine. Thread-safe.
 *
 * @param volume Underlying database.
 */
class Runtime(volume: Volume) extends Serializable {

  /**
   * Executes the program and returns the result. Programs are repeatedly partially evaluated until
   * they are reduced to a single literal value. Automatically batches reads and buffers writes.
   *
   * @param program Program to execute.
   * @throws Rollbacked If the program was rolled back.
   * @throws Aborted If the program could not be executed.
   * @throws Fault If the program is illegally constructed.
   * @return Literal result or exception on failure.
   */
  def execute(program: Program): Try[Literal] = {
    val versions = mutable.Map.empty[Key, Version].withDefaultValue(0L)
    val snapshot = mutable.Map.empty[Key, Literal].withDefaultValue(Null)
    val depends = mutable.Map.empty[Key, Version]
    val buffer = mutable.Map.empty[Key, Literal]
    val locals = mutable.Map.empty[Key, Literal]

    @tailrec
    def reduce(iteration: Program): Try[Literal] = {
      // Translate the program into a key.
      @tailrec
      def key(x: Program): Option[String] = x match {
        case Null => None
        case l: Literal => Some(l.asString)
        case e: Expression => key(evaluate(List(e), List.empty))
      }

      // Fetch all keys that are read (for their value) and written (for their version), that have
      // not been read before (to avoid changes in value and version) to ensure that the evaluation
      // of an expression is correct and consistent.
      @tailrec
      def fetch(stack: List[Program], keys: Set[Key]): Set[Key] = stack match {
        case Nil => keys
        case (_: Literal) :: rest => fetch(rest, keys)
        case Expression(Read, x :: Nil) :: rest => fetch(x :: rest, keys ++ key(x))
        case Expression(Write, x :: _) :: rest => fetch(x :: rest, keys ++ key(x))
        case (o: Expression) :: rest => fetch(o.operands ::: rest, keys)
      }

      val keys = fetch(List(iteration), Set.empty) -- versions.keys
      val revisions = if (keys.isEmpty) Success(Map.empty) else this.volume.get(keys)

      revisions match {
        case Success(x) =>
          // Add the returned revisions to the snapshot.
          versions ++= x.mapValues(r => r.version)
          snapshot ++= x.mapValues(r => Literal(r.value))

          // Evaluate the program. If the result is a literal, then return it. Otherwise, recurse.
          evaluate(List(iteration), List.empty) match {
            case l: Literal => Success(l)
            case o: Expression => reduce(o)
          }
        case Failure(e) => Failure(e)
      }
    }

    @tailrec
    def evaluate(stack: List[Any], results: List[Program]): Program = (stack, results) match {
      // Return Results.
      case (Nil, _) =>
        if (results.size != 1)
          throw Fault(s"Transaction evaluates to $results.")
        else
          results.head

      // Replace Literals.
      case ((l: Literal) :: rest, rem) => evaluate(rest, l :: rem)

      // Expand Expressions.
      case (Expression(Read, Null :: Nil) :: rest, rem) =>
        evaluate(rest, Null :: rem)
      case (Expression(Read, (k: Literal) :: Nil) :: rest, rem) =>
        depends += k.asString -> versions(k.asString)
        evaluate(rest, buffer.getOrElse(k.asString, snapshot(k.asString)) :: rem)
      case (Expression(Write, Text(k) :: (v: Literal) :: Nil) :: rest, rem) =>
        depends += k -> versions(k)
        buffer += k -> v
        evaluate(rest, Null :: rem)
      case (Expression(Branch, cmp :: pass :: fail :: Nil) :: rest, rem) =>
        evaluate(cmp :: Branch :: rest, pass :: fail :: rem)
      case (Expression(Cons, first :: second :: Nil) :: rest, rem) =>
        evaluate(first :: Cons :: rest, second :: rem)
      case (Expression(Repeat, c :: b :: Nil) :: rest, rem) =>
        evaluate(branch(c, cons(b, repeat(c, b)), Null) :: rest, rem)
      case ((e: Expression) :: rest, rem) =>
        evaluate(e.operands.reverse ::: e.operator :: rest, rem)

      // Simplify Core Expressions.
      case (Read :: rest, k :: rem) => evaluate(rest, read(k) :: rem)
      case (Write :: rest, k :: v :: rem) => evaluate(rest, write(k, v) :: rem)
      case (Load :: rest, Text(k) :: rem) => evaluate(rest, locals.getOrElse(k, Null) :: rem)
      case (Load :: rest, k :: rem) => evaluate(rest, load(k) :: rem)
      case (Store :: rest, Text(k) :: (v: Literal) :: rem) => locals += k -> v; evaluate(rest, v :: rem)
      case (Store :: rest, k :: v :: rem) => evaluate(rest, store(k, v) :: rem)
      case (Rollback :: _, (l: Literal) :: _) => throw Rollbacked(l)
      case (Rollback :: rest, x :: rem) => evaluate(rest, rollback(x) :: rem)
      case (Repeat :: rest, False :: _ :: rem) => evaluate(rest, rem)
      case (Repeat :: rest, c :: b :: rem) => evaluate(rest, repeat(c, b) :: rem)
      case (Prefetch :: rest, k :: s :: r :: rem) => evaluate(rest, prefetch(k, s, r) :: rem)
      case (Cons :: rest, (_: Literal) :: s :: rem) => evaluate(s :: rest, rem)
      case (Cons :: rest, f :: s :: rem) => evaluate(rest, cons(f, s) :: rem)
      case (Branch :: rest, True :: pass :: _ :: rem) => evaluate(pass :: rest, rem)
      case (Branch :: rest, False :: _ :: fail :: rem) => evaluate(fail :: rest, rem)
      case (Branch :: rest, Null :: _ :: fail :: rem) => evaluate(fail :: rest, rem)
      case (Branch :: rest, (_: Literal) :: pass :: _ :: rem) => evaluate(pass :: rest, rem)
      case (Branch :: rest, c :: p :: f :: rem) => evaluate(rest, branch(c, p, f) :: rem)

      // Simplify String Expressions.
      case (Length :: rest, x :: rem) => evaluate(rest, length(x) :: rem)
      case (Matches :: rest, x :: y :: rem) => evaluate(rest, matches(x, y) :: rem)
      case (Contains :: rest, x :: y :: rem) => evaluate(rest, contains(x, y) :: rem)
      case (Slice :: rest, x :: l :: h :: rem) => evaluate(rest, slice(x, l, h) :: rem)
      case (IndexOf :: rest, x :: y :: rem) => evaluate(rest, indexOf(x, y) :: rem)

      // Simplify Math Expressions.
      case (Add :: rest, x :: y :: rem) => evaluate(rest, add(x, y) :: rem)
      case (Sub :: rest, x :: y :: rem) => evaluate(rest, sub(x, y) :: rem)
      case (Mul :: rest, x :: y :: rem) => evaluate(rest, mul(x, y) :: rem)
      case (Div :: rest, x :: y :: rem) => evaluate(rest, div(x, y) :: rem)
      case (Mod :: rest, x :: y :: rem) => evaluate(rest, mod(x, y) :: rem)
      case (Pow :: rest, x :: y :: rem) => evaluate(rest, pow(x, y) :: rem)
      case (Log :: rest, x :: rem) => evaluate(rest, log(x) :: rem)
      case (Sin :: rest, x :: rem) => evaluate(rest, sin(x) :: rem)
      case (Cos :: rest, x :: rem) => evaluate(rest, cos(x) :: rem)
      case (Floor :: rest, x :: rem) => evaluate(rest, floor(x) :: rem)

      // Simplify Logical Expressions.
      case (Both :: rest, x :: y :: rem) => evaluate(rest, both(x, y) :: rem)
      case (Either :: rest, x :: y :: rem) => evaluate(rest, either(x, y) :: rem)
      case (Negate :: rest, x :: rem) => evaluate(rest, negate(x) :: rem)

      // Simplify Comparison Expressions.
      case (Equal :: rest, x :: y :: rem) => evaluate(rest, equal(x, y) :: rem)
      case (Less :: rest, x :: y :: rem) => evaluate(rest, less(x, y) :: rem)

      // Default Error.
      case _ => throw Fault(s"$stack cannot be applied to $results.")
    }

    // Recursively evaluate the program, and then conditionally persist all changes made by the
    // transaction to the underlying database if and only if the versions of its various
    // dependencies have not changed. Filter out empty first changes to allow local variables.
    reduce(program) recoverWith { case e: Rollbacked =>
      this.volume.cas(depends.toMap, Map.empty) match {
        case Success(_) => Failure(e)
        case _ => Failure(Aborted)
      }
    } flatMap { r =>
      this.volume.cas(depends.toMap, buffer.mapValues(_.asBinary).toMap) match {
        case Success(_) => Success(r)
        case _ => Failure(Aborted)
      }
    }
  }

}

object Runtime {

  /**
   * A non-retryable failure that terminates execution and discards all writes.
   *
   * @param message Literal return value.
   */
  case class Rollbacked(message: Literal) extends Exception

  /**
   * A retryable failure that indicates a program could not be transactionally executed.
   */
  case object Aborted extends Exception

  /**
   * An non-retryable failure that is thrown when a program is illegally constructed.
   *
   * @param message Error message.
   */
  case class Fault(message: String) extends Exception

  /**
   * Constructs a runtime connected to the specified database.
   *
   * @param database Underlying database.
   * @return Initialized runtime.
   */
  def apply(database: Volume): Runtime = new Runtime(database)

}