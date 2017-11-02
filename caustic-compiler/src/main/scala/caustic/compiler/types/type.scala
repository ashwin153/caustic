package caustic.compiler.types

/**
 * A type hierarchy.
 */
sealed trait Type

/**
 * An undefined compiler [[Type]].
 */
case object Undefined extends Type

/**
 * A pointer to another [[Type]]. Pointers correspond to a remote key-value pair within a
 * [[caustic.runtime.thrift.Transaction]] and are the targets of [[caustic.runtime.thrift.Read]] and
 * [[caustic.runtime.thrift.Write]] expressions. Because each pointer dereference requires an
 * additional database read, nested pointers are explicitly forbidden.
 *
 * @param datatype Underlying [[Type]].
 */
case class Pointer(datatype: Simple) extends Type

/**
 * A non-pointer [[Type]].
 */
sealed trait Simple extends Type

/**
 * An object. Records contain fields that are each associated with a unique name and a type
 * [[Alias]]. Records are structurally typed; therefore, records with the same fields are equivalent
 * and a record is a subclass of another record if its fields are a subset of the other.
 *
 * @param fields Contained fields.
 */
case class Record(fields: Map[String, Alias]) extends Simple

/**
 * A value [[Type]]. Primitives are ordered in descending order of precedence.
 */
sealed trait Primitive extends Simple
case object Null extends Primitive
case object String extends Primitive
case object Double extends Primitive
case object Int extends Primitive
case object Boolean extends Primitive

