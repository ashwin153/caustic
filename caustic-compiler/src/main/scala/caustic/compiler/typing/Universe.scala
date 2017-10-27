package caustic.compiler.typing

import scala.collection.mutable

/**
 * A scoped symbol table.
 *
 * @param symbols Defined symbols.
 * @param labels Lexical scope.
 */
case class Universe(
  symbols: mutable.Map[String, Symbol],
  labels: List[String]
) {

  // Monotonically increasing label counter.
  private var label: Int = -1

  /**
   * Returns the name in the current Universe.
   *
   * @return Scoped name.
   */
  def scope(name: String): String =
    (this.labels :+ name).mkString("@")

  /**
   * Returns the Universe that created this Universe, or None if this Universe is the root.
   *
   * @return Parent Universe.
   */
  def parent: Option[Universe] =
    if (this.labels.size == 1) None else Some(Universe(this.symbols, this.labels.dropRight(1)))

  /**
   * Returns a Universe that inherits all existing symbols from this Universe. Any modifications to
   * the child Universe will not be visible in this Universe. Child universe is assigned a
   * monotonically increasing label number that uniquely identifies it.
   *
   * @return Child Universe.
   */
  def child(): Universe = {
    this.label += 1
    Universe(mutable.Map.empty ++ this.symbols, this.labels :+ this.label.toString)
  }

  /**
   * Returns a Universe that inherits all existing symbols from this Universe. Any modifications to
   * the child Universe will not be visible in this Universe.
   *
   * @param module Unique module name.
   * @return Child Universe.
   */
  def child(module: Seq[String]): Universe =
    Universe(mutable.Map.empty ++ this.symbols, this.labels ++ module)

  /**
   * Returns the Symbol with the corresponding name.
   *
   * @param name Symbol name.
   * @return Corresponding Symbol.
   */
  def getSymbol(name: String): Symbol =
    this.symbols.getOrElse(scope(name), this.parent.get.getSymbol(name))

  /**
   * Returns the Type with the corresponding name.
   *
   * @param name Type name.
   * @return Corresponding Type.
   */
  def getType(name: String): Type =
    getSymbol(name).asInstanceOf[Type]

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
  def putFunction(name: String, args: Map[String, Type], body: Result): Unit =
    this.symbols += scope(name) -> Function(args, body)

  /**
   * Returns the Object with the specified name.
   *
   * @param name Object name.
   * @return Corresponding Object.
   */
  def getObject(name: String): Object =
    getSymbol(name).asInstanceOf[Object]

  /**
   * Constructs an Object and adds it to the Universe.
   *
   * @param name Object name.
   * @param fields Object fields.
   */
  def putObject(name: String, fields: Map[String, Type]): Unit =
    this.symbols += scope(name) -> Object(fields)

}

object Universe {

  /**
   * Returns the root Universe.
   *
   * @return Root Universe.
   */
  val root: Universe = Universe(mutable.Map.empty, List("root"))

}