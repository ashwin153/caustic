package caustic.syntax.scala

/**
 *
 */
sealed trait Type
case class Primitive(name: String) extends Type
case class Function(args: Map[String, Type], returns: String, result: String) extends Type
case class Record(fields: Map[String, Type]) extends Type
case class Service(functions: Map[String, Function]) extends Type

/**
 *
 */
sealed trait Symbol
case class Variable(name: String, of: Type)
case class Reference(key: String, to: Type)