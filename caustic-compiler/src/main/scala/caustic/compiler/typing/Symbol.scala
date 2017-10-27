package caustic.compiler.typing

/**
 *
 */
sealed trait Symbol
case class Variable(datatype: Type, name: String) extends Symbol
case class Function(args: Map[String, Type], returns: Result) extends Symbol
sealed trait Type extends Symbol
case class Pointer(to: Basic) extends Type
sealed trait Basic extends Type
case object Primitive extends Basic
case class Object(fields: Map[String, Type]) extends Basic