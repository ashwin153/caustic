package caustic.syntax.core

// Symbol Table
//
//"String" -> Type

//Declarations
//"String" -> Primitive
//"String.charAt" -> Function(Record("String"), Record("Integer"), slice(load("i"), 0))
//"String.indexOf" -> Function(Record("Integer"))
//
// Foo    -> Function(Record("Foo"), Primitive("String"))
//"Foo.a" -> Record("Foo")
//"Foo.b" -> Primitive("String")
//"Foo.c" -> Reference("Bar")
//"Foo.d" -> Record("Bar")
//"Bar.a" -> Record("Decimal")
//
//Symbols
//"x" -> Variable(load("__namespace__") + "x") @@ String
//"y" -> Variable(load("__namespace__") + "y")


//"z" -> Record("Foo")
//
//"x.a.charAt"
//
//"x" -> Record("Foo")
//"Foo.a" -> Record("String")
//"String.charAt" -> Function

/**
 *
 */
sealed trait Symbol
case class Reference(key: Transaction, datatype: Symbol) extends Symbol
case class Variable(name: String, datatype: Symbol) extends Symbol
case class Function(returns: Symbol, args: List[Variable]) extends Symbol
case class Record(params: List[Variable]) extends Symbol

//// Basic Types.
//case class Primitive(name: String) extends Symbol
//case class Attribute(name: String, datatype: Symbol) extends Symbol
//case class Structure(name: String, attributes: List[Attribute]) extends Symbol
//case class Reference(name: String, key: Transaction) extends Symbol
//
//// Complex Types.
//case class Constant(value: Transaction, datatype: Primitive) extends Symbol
//case class Record(key: Transaction, schema: Structure) extends Symbol
//case class Variable(name: Transaction, datatype: Symbol) extends Symbol
//case class Function(result: Transaction, returns: Symbol, args: List[Variable]) extends Symbol
