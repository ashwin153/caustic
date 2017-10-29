package caustic.compiler.types

/**
 *
 */
sealed trait Symbol
case class Variable(datatype: Type, name: String) extends Symbol
case class Function(args: Map[String, Type], returns: Result) extends Symbol

/**
 *
 */
sealed trait Type extends Symbol
case class Pointer(datatype: Simple) extends Type

/**
 *
 */
sealed trait Simple extends Type
case class Record(fields: Map[String, Type]) extends Simple

/**
 *
 */
sealed trait Primitive extends Simple
case object Boolean extends Primitive
case object Integer extends Primitive
case object Decimal extends Primitive
case object Textual extends Primitive
