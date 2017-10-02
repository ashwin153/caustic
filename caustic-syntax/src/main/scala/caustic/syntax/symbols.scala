package caustic.syntax

/**
 *
 */
sealed trait Symbol
case class Variable(name: String, mutable: Boolean, of: Type) extends Symbol
case class Function(args: List[Variable], returns: Type, body: String) extends Symbol
case class Service(constants: List[Variable], functions: List[Function]) extends Symbol

/**
 *
 */
sealed trait Type extends Symbol
case object Primitive extends Type
case class Reference(to: Type) extends Type
case class Record(fields: Map[String, Type]) extends Type