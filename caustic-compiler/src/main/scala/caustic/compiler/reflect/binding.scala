package caustic.compiler.reflect

/**
 * A compiler binding. Bindings store static type information that the compiler uses to perform type
 * inference. Caustic has a relatively simple static type system. A type is a binding that may
 * be the result of an expression. A simple type may be passed to or returned by a function or be
 * used as a field of a struct. A record type may have fields. A built-in type is a compiler
 * provided record and may represent either a primitive value or a collection of values. Programs
 * may define four kinds of bindings. A variable binds a name to a type. A struct binds a name to a
 * collection of fields. A function binds a name to a list of arguments and a return type all of
 * simple type. A service binds a name to a collection of functions.
 */
sealed trait Binding
sealed trait Type extends Binding
sealed trait Simple extends Type
sealed trait Record extends Simple { def fields: Map[String, Type] }
sealed trait BuiltIn extends Record
sealed trait Primitive extends BuiltIn
sealed trait Collection extends BuiltIn

// Null
case object CUnit extends Primitive {
  override def toString: String = "Unit"
  override val fields: Map[String, Type] = Map.empty
}

// Boolean
case object CBoolean extends Primitive {
  override def toString: String = "Boolean"
  override val fields: Map[String, Type] = Map.empty
}

// Int
case object CInt extends Primitive {
  override def toString: String = "Int"
  override val fields: Map[String, Type] = Map(
    "max"         -> CFunction("max", List(CInt), CInt),
    "min"         -> CFunction("min", List(CInt), CInt)
  )
}

// Double
case object CDouble extends Primitive {
  override def toString: String = "Double"
  override val fields: Map[String, Type] = Map(
    "max"         -> CFunction("max", List(CDouble), CDouble),
    "min"         -> CFunction("min", List(CDouble), CDouble)
  )
}

// String
case object CString extends Primitive {
  override def toString: String = "String"
  override val fields: Map[String, Type] = Map(
    "charAt"      -> CFunction("charAt", List(CInt), CString),
    "contains"    -> CFunction("contains", List(CString), CBoolean),
    "indexOf"     -> CFunction("indexOf", List(CString), CInt),
    "length"      -> CInt,
    "matches"     -> CFunction("matches", List(CString), CBoolean),
    "quoted"      -> CString,
    "substring"   -> CFunction("substring", List(CInt, CInt), CString)
  )
}

// List
case class CList(value: Primitive) extends Collection {
  override def toString: String = s"List[$value]"
  override def fields: Map[String, Type] = Map(
    "contains"    -> CFunction("contains", List(value), CBoolean),
    "get"         -> CFunction("apply", List(CInt), value),
    "find"        -> CFunction("find", List(value), CInt),
    "indexOf"     -> CFunction("indexOf", List(value), CInt),
    "remove"      -> CFunction("remove", List(value), CUnit),
    "set"         -> CFunction("set", List(CInt, value), CUnit),
    "size"        -> CInt
  )
}

// Set
case class CSet(value: Primitive) extends Collection {
  override def toString: String = s"Set[$value]"
  override def fields: Map[String, Type] = Map(
    "add"         -> CFunction("add", List(value), CUnit),
    "contains"    -> CFunction("contains", List(value), CBoolean),
    "diff"        -> CFunction("diff", List(CSet(value)), CSet(value)),
    "intersect"   -> CFunction("intersect", List(CSet(value)), CSet(value)),
    "remove"      -> CFunction("remove", List(value), CUnit),
    "size"        -> CInt,
    "union"       -> CFunction("union", List(CSet(value)), CSet(value))
  )
}

// Map
case class CMap(key: Primitive, value: Primitive) extends Collection {
  override def toString: String = s"Map[$key, $value]"
  override def fields: Map[String, Type] = Map(
    "get"         -> CFunction("apply", List(CInt), value),
    "exists"      -> CFunction("exists", List(key), CBoolean),
    "find"        -> CFunction("find", List(value), CInt),
    "keys"        -> CSet(key),
    "remove"      -> CFunction("remove", List(value), CUnit),
    "set"         -> CFunction("set", List(key, value), CUnit),
    "size"        -> CInt
  )
}

// struct
case class CStruct(fields: Map[String, Simple]) extends Record

// &
case class CPointer(to: Record) extends Simple

// var
case class CVariable(of: Simple) extends Binding

// def
case class CFunction(name: String, args: List[Simple], returns: Simple) extends Type

// service
case class CService(functions: Map[String, CFunction]) extends Binding