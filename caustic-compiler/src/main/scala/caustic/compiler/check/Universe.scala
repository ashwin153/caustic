package caustic.compiler.check

import scala.collection.mutable

/**
 * A collection of all defined symbols.
 *
 * @param symbols Symbol table.
 * @param labels Lexical scope.
 */
case class Universe(
  symbols: mutable.Map[String, Symbol],
  labels: List[String]
) {

  // Monotonically increasing label counter.
  private var label: Int = -1

  /**
   * Returns the corresponding name in the current Universe.
   *
   * @return Scoped name.
   */
  def scope(name: String): String =
    (this.labels :+ name).mkString("@")

  /**
   * Returns the Universe that created this Universe.
   *
   * @return Parent Universe.
   */
  def parent: Option[Universe] =
    if (this.labels.isEmpty) None else Some(Universe(this.symbols, this.labels.dropRight(1)))

  /**
   * Returns a Universe that inherits all existing symbols from this Universe. Any modifications to
   * the child Universe will not be visible in this Universe.
   *
   * @param module Unique module name.
   * @return Child Universe.
   */
  def child(module: String*): Universe =
    if (module.isEmpty) {
      // If no module is specified, then assign a monotonically increasing label.
      this.label += 1
      Universe(this.symbols, this.labels :+ this.label.toString)
    } else {
      Universe(this.symbols, this.labels ++ module)
    }

  /**
   * Returns the Symbol with the corresponding name.
   *
   * @param name Symbol name.
   * @return Corresponding Symbol.
   */
  def getSymbol(name: String): Symbol =
    this.symbols.getOrElse(scope(name), this.parent.get.getSymbol(name))

  /**
   * Returns the Variable with the corresponding name.
   *
   * @param name Variable name.
   * @return Corresponding Variable.
   */
  def getVariable(name: String): Variable =
    getSymbol(name).asInstanceOf[Variable]

  /**
   * Constructs a Variable and adds it to the Universe.
   *
   * @param name Variable name.
   * @param datatype Variable type.
   */
  def putVariable(name: String, datatype: Type): Unit =
    this.symbols += scope(name) -> Variable(datatype, scope(name))

  /**
   * Returns the Function with the specified name.
   *
   * @param name Function Name.
   * @return Corresponding Function.
   */
  def getFunction(name: String): Function =
    getSymbol(name).asInstanceOf[Function]

  /**
   * Constructs a Function and adds it to the Universe.
   *
   * @param name Function name.
   * @param args Function arguments.
   * @param body Function body.
   */
  def putFunction(name: String, args: Map[String, Type], body: Universe => Result): Unit = {
    // Add all arguments to the function's universe.
    val context = this.child(name)
    args foreach { case (n, t) => context.putVariable(n, t) }

    // Evaluate the body of the function and add it to the universe.
    val scoped = args map { case (n, t) => context.scope(n) -> t }
    this.symbols += scope(name) -> Function(scoped, body(context))
  }

  /**
   * Returns the type with the corresponding name.
   *
   * @param name Type name.
   * @return Corresponding Type.
   */
  def getType(name: String): Type =
    if (name.endsWith("&"))
      Pointer(getType(name.dropRight(1)).asInstanceOf[Simple])
    else
      getSymbol(name + "$Type").asInstanceOf[Type]

  /**
   * Adds a simple type to the universe.
   *
   * @param name Type name.
   * @param simple Type symbol.
   */
  def putType(name: String, simple: Simple): Unit = {
    // Add the type to the universe.
    this.symbols += scope(name + "$Type") -> simple

    // Add a pointer constructor to the universe.
    val args = Map("key" -> Textual)
    val body = (u: Universe) => Result(Pointer(simple), s"""load("${ u.scope("key") })")""")
    putFunction(name + "&",  args, body)
  }

  /**
   * Returns the record with the specified name.
   *
   * @param name Record name.
   * @return Corresponding record.
   */
  def getRecord(name: String): Record =
    getType(name).asInstanceOf[Record]

  /**
   * Constructs a record and adds it to the universe.
   *
   * @param name Record name.
   * @param fields Record fields.
   */
  def putRecord(name: String, fields: Map[String, Type]): Unit = {
    // Add a record type and a corresponding constructor.
    putType(name, Record(fields))
    putFunction(name, fields, u => Result(Record(fields), s"""text("${ u.labels.mkString("@") }")"""))
  }

}

object Universe {

  /**
   * Returns the root Universe.
   *
   * @return Root Universe.
   */
  val root: Universe = {
    val base = Universe(mutable.Map.empty, List.empty)
    base.putType("Boolean", Boolean)
    base.putType("Int", Integer)
    base.putType("Double", Decimal)
    base.putType("String", Textual)
    base.child("root")
  }

}