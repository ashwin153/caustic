package caustic.compiler.reflect

/**
 * A compiler binding.
 */
sealed trait Binding
case class Service(functions: Map[String, Function]) extends Binding
case class Variable(of: Value) extends Binding

/**
 * A type definition.
 */
sealed trait Type extends Binding
case class Function(name: String, args: List[Value], returns: Value) extends Type

/**
 * A value type.
 */
sealed trait Value extends Type
case class Pointer(to: Struct) extends Value

/**
 * A non-pointer type.
 */
sealed trait Struct extends Value {

  /**
   *
   * @return
   */
  def fields: Map[String, Type]

}

case class Defined(fields: Map[String, Value]) extends Struct
sealed trait BuiltIn extends Struct
sealed trait Primitive extends BuiltIn
sealed trait Collection extends BuiltIn

// Built-In Types.
case object CUnit extends Primitive {
  override def toString: String = "Unit"
  override val fields: Map[String, Type] = Map.empty
}

case object CBoolean extends Primitive {
  override def toString: String = "Boolean"
  override val fields: Map[String, Type] = Map.empty
}

case object CInt extends Primitive {
  override def toString: String = "Int"
  override val fields: Map[String, Type] = Map(
    "max"         -> Function("max", List(CInt), CInt),
    "min"         -> Function("min", List(CInt), CInt)
  )
}

case object CDouble extends Primitive {
  override def toString: String = "Double"
  override val fields: Map[String, Type] = Map(
    "max"         -> Function("max", List(CDouble), CDouble),
    "min"         -> Function("min", List(CDouble), CDouble)
  )
}

case object CString extends Primitive {
  override def toString: String = "String"
  override val fields: Map[String, Type] = Map(
    "charAt"      -> Function("charAt", List(CInt), CString),
    "contains"    -> Function("contains", List(CString), CBoolean),
    "indexOf"     -> Function("indexOf", List(CString), CInt),
    "length"      -> CInt,
    "matches"     -> Function("matches", List(CString), CBoolean),
    "quoted"      -> CString,
    "substring"   -> Function("substring", List(CInt, CInt), CString)
  )
}

case class CList(value: Primitive) extends Collection {
  override def toString: String = s"List[$value]"
  override val fields: Map[String, Type] = Map(
    "append"      -> Function("append", List(value), CUnit),
    "apply"       -> Function("apply", List(CInt), value),
    "contains"    -> Function("contains", List(value), CBoolean),
    "indexOf"     -> Function("indexOf", List(value), CInt),
    "remove"      -> Function("remove", List(value), CUnit),
    "size"        -> CInt
  )
}

case class CSet(value: Primitive) extends Collection {
  override def toString: String = s"Set[$value]"
  override val fields: Map[String, Type] = Map(
    "add"         -> Function("add", List(value), CUnit),
    "contains"    -> Function("contains", List(value), CBoolean),
    "indexOf"     -> Function("indexOf", List(value), CInt),
    "remove"      -> Function("remove", List(value), CUnit),
    "size"        -> CInt
  )
}

case class CMap(key: Primitive, value: Primitive) extends Collection {
  override def toString: String = s"Map[$key, $value]"
  override val fields: Map[String, Type] = Map(
    "apply"       -> Function("apply", List(key), value),
    "contains"    -> Function("contains", List(key), CBoolean),
    "put"         -> Function("indexOf", List(key, value), CInt),
    "putIfAbsent" -> Function("remove", List(key, value), CUnit),
    "remove"      -> Function("remove", List(key), CUnit),
    "size"        -> CInt
  )
}
