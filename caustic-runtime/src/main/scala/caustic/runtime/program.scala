package caustic.runtime

import java.io._
import java.nio.charset.Charset

/**
 * A transactional procedure. Programs are an abstract-syntax tree composed of [[Literal]] leaves
 * and [[Expression]] nodes.
 */
sealed trait Program

/**
 * A [[Literal]] transformation. Expressions map [[Literal]] operands to a [[Literal]] result
 * using their [[Operator]]. Because all expressions produce [[Literal]] results on [[Literal]]
 * arguments, every [[Program]] can eventually be reduced to a single [[Literal]].
 *
 * @param operator
 * @param operands
 */
case class Expression(operator: Operator, operands: List[Program]) extends Program

/**
 * A typed value. Literals contain primitive values of type [[Null]], [[Flag]], [[Real]], and
 * [[Text]] which correspond to null, boolean, double, and string respectively.
 */
sealed trait Literal extends Program with Serializable
case object Null extends Literal
case class Flag(value: Boolean) extends Literal
case class Real(value: Double) extends Literal
case class Text(value: String) extends Literal

object Literal {

  // Default character representation.
  val charset: Charset = Charset.forName("UTF-8")

  /**
   *
   * @param repr
   * @return
   */
  def deserialize(repr: String): Literal = {
    val bytes  = new ByteArrayInputStream(repr.getBytes(this.charset))
    val stream = new ObjectInputStream(bytes)
    stream.readObject().asInstanceOf[Literal]
  }

  /**
   *
   * @param literal
   * @return
   */
  def serialize(literal: Literal): String = {
    val bytes  = new ByteArrayOutputStream()
    val stream = new ObjectOutputStream(bytes)
    stream.writeObject(literal)
    new String(bytes.toByteArray, this.charset)
  }

}


//object Program {
//
//  /**
//   * Constructs a [[Program]] from the specified Thrift representation.
//   *
//   * @param program Thrift program.
//   * @return Scala program.
//   */
//  def parse(program: thrift.Program): Program = {
//    @tailrec
//    def convert(stack: List[Any], processed: List[Program]): Program = stack match {
//      case Nil =>
//        processed.head
//      case (t: thrift.Program) :: rest if t.isSetLiteral =>
//        val literal = t.getLiteral match {
//          case l if l.isSetFlag => flag(l.getFlag)
//          case l if l.isSetReal => real(l.getReal)
//          case l if l.isSetText => text(l.getText)
//          case l if l.isSetNone => Null
//        }
//
//        convert(rest, literal :: processed)
//      case (t: thrift.Program) :: rest if t.isSetExpression =>
//        val (operator, operands) = t.getExpression match {
//          case e if e.isSetRead => (Read, e.getRead.key :: Nil)
//          case e if e.isSetWrite => (Write, e.getWrite.key :: e.getWrite.value :: Nil)
//          case e if e.isSetLoad => (Load, e.getLoad.variable :: Nil)
//          case e if e.isSetStore => (Store, e.getStore.variable :: e.getStore.value :: Nil)
//          case e if e.isSetCons => (Cons, e.getCons.first :: e.getCons.second :: Nil)
//          case e if e.isSetRepeat => (Repeat, e.getRepeat.condition :: e.getRepeat.body :: Nil)
//          case e if e.isSetBranch => (Branch, e.getBranch.condition :: e.getBranch.success :: e.getBranch.failure :: Nil)
//          case e if e.isSetRollback => (Rollback, e.getRollback.message :: Nil)
//          case e if e.isSetRandom => (Random, Nil)
//          case e if e.isSetAdd => (Add, e.getAdd.x :: e.getAdd.y :: Nil)
//          case e if e.isSetSub => (Sub, e.getSub.x :: e.getSub.y :: Nil)
//          case e if e.isSetMul => (Mul, e.getMul.x :: e.getMul.y :: Nil)
//          case e if e.isSetDiv => (Div, e.getDiv.x :: e.getDiv.y :: Nil)
//          case e if e.isSetMod => (Mod, e.getMod.x :: e.getMod.y :: Nil)
//          case e if e.isSetPow => (Pow, e.getPow.x :: e.getPow.y :: Nil)
//          case e if e.isSetLog => (Log, e.getLog.x :: Nil)
//          case e if e.isSetSin => (Sin, e.getSin.x :: Nil)
//          case e if e.isSetCos => (Cos, e.getCos.x :: Nil)
//          case e if e.isSetFloor => (Floor, e.getFloor.x :: Nil)
//          case e if e.isSetBoth => (Both, e.getBoth.x :: e.getBoth.y :: Nil)
//          case e if e.isSetEither => (Either, e.getEither.x :: e.getEither.y :: Nil)
//          case e if e.isSetNegate => (Negate, e.getNegate.x :: Nil)
//          case e if e.isSetLength => (Length, e.getLength.x :: Nil)
//          case e if e.isSetSlice => (Slice, e.getSlice.x :: e.getSlice.lower :: e.getSlice.higher :: Nil)
//          case e if e.isSetMatches => (Matches, e.getMatches.x :: e.getMatches.regex :: Nil)
//          case e if e.isSetContains => (Contains, e.getContains.x :: e.getContains.y :: Nil)
//          case e if e.isSetEqual => (Equal, e.getEqual.x :: e.getEqual.y :: Nil)
//          case e if e.isSetLess => (Less, e.getLess.x :: e.getLess.y :: Nil)
//        }
//
//        convert(operands.reverse ::: operator :: rest, processed)
//      case (op: Operator) :: rest =>
//        (op, processed) match {
//          case (Read, k :: rem) => convert(rest, read(k) :: rem)
//          case (Write, k :: v :: rem) => convert(rest, write(k, v) :: rem)
//          case (Load, k :: rem) => convert(rest, load(k) :: rem)
//          case (Store, k :: v :: rem) => convert(rest, store(k, v) :: rem)
//          case (Cons, x :: y :: rem) => convert(rest, cons(x, y) :: rem)
//          case (Repeat, c :: b :: rem) => convert(rest, repeat(c, b) :: rem)
//          case (Branch, c :: p :: f :: rem) => convert(rest, branch(c, p, f) :: rem)
//          case (Rollback, m :: rem) => convert(rest, rollback(m) :: rem)
//          case (Random, rem) => convert(rest, random() :: rem)
//          case (Add, x :: y :: rem) => convert(rest, add(x, y) :: rem)
//          case (Sub, x :: y :: rem) => convert(rest, sub(x, y) :: rem)
//          case (Mul, x :: y :: rem) => convert(rest, mul(x, y) :: rem)
//          case (Div, x :: y :: rem) => convert(rest, div(x, y) :: rem)
//          case (Mod, x :: y :: rem) => convert(rest, mod(x, y) :: rem)
//          case (Pow, x :: y :: rem) => convert(rest, pow(x, y) :: rem)
//          case (Log, x :: rem) => convert(rest, log(x) :: rem)
//          case (Sin, x :: rem) => convert(rest, sin(x) :: rem)
//          case (Cos, x :: rem) => convert(rest, cos(x) :: rem)
//          case (Floor, x :: rem) => convert(rest, floor(x) :: rem)
//          case (Both, x :: y :: rem) => convert(rest, both(x, y) :: rem)
//          case (Either, x :: y :: rem) => convert(rest, either(x, y) :: rem)
//          case (Negate, x :: rem) => convert(rest, negate(x) :: rem)
//          case (Equal, x :: y :: rem) => convert(rest, equal(x, y) :: rem)
//          case (Less, x :: y :: rem) => convert(rest, less(x, y) :: rem)
//          case (Length, x :: rem) => convert(rest, length(x) :: rem)
//          case (Slice, x :: l :: h :: rem) => convert(rest, slice(x, l, h) :: rem)
//          case (Matches, x :: y :: rem) => convert(rest, matches(x, y) :: rem)
//          case (Contains, x :: y :: rem) => convert(rest, contains(x, y) :: rem)
//          case (IndexOf, x :: y :: rem) => convert(rest, indexOf(x, y) :: rem)
//          case _ => throw new thrift.ExecutionException(s"Unable to parse $op given $processed")
//        }
//
//    }
//
//    convert(List(program), List.empty)
//  }
//
//}