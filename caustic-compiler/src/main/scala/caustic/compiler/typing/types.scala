package caustic.compiler.typing

/**
 * A name binding.
 */
sealed trait Binding

/**
 * A type binding.
 */
sealed trait Kind extends Binding {

  /**
   * Returns the name of the type.
   *
   * @return Name.
   */
  def name: String

}

/**
 * A pointer to a simple type. Pointers generate Reference[$name](Variable.Remote(rhs)).
 *
 * @param kind Underlying type.
 */
case class Pointer(kind: Struct) extends Kind {
  override val name: String = kind.name
}

/**
 * A primitive kind. Primitives generate Variable.Local[$name](context.label()).
 *
 * @param name Type name.
 */
sealed abstract class Primitive(override val name: String) extends Kind
case object CString extends Primitive("String")
case object CDouble extends Primitive("Double")
case object CInt extends Primitive("Int")
case object CBoolean extends Primitive("Boolean")
case object CUnit extends Primitive("Unit")

/**
 * An object kind. Structs generate Reference[$name](Variable.Local(context.label())).
 *
 * @param name Type name.
 * @param fields Fields.
 */
case class Struct(override val name: String, fields: Map[String, Kind]) extends Kind

/**
 * A collection of functions.
 *
 * @param functions Members.
 */
case class Service(functions: Map[String, Function]) extends Binding

/**
 * A function binding.
 *
 * @param arguments Operands.
 * @param returns Result.
 */
case class Function(arguments: Map[String, Kind], returns: Result) extends Binding

/**
 * A variable binding.
 *
 * @param kind Type.
 */
case class Variable(kind: Kind) extends Binding