package caustic.compiler.goals

import caustic.compiler.Goal
import caustic.compiler.types._
import caustic.grammar.CausticParser

import scala.util.Try

/**
 * A Thrift IDL generator. Requires the [[Declare]] goal to add all declarations in the program to
 * the [[Universe]].
 */
object GenThrift extends Goal[String] {

  override def execute(parser: CausticParser): Try[String] = {
    // Determine the Thrift namespace of the program.
    val program = parser.program()
    val namespace = if (program.Module() != null) program.module(0).getText else ""

    Try(Declare(Universe.root).visitProgram(program)) map { universe =>
      // Convert all record aliases in the universe to Thrift structs.
      val structs = universe.aliases.values collect {
        case Alias(name, Record(fields)) =>
          s"""struct $name {
             |  ${ GenThrift.serialize(fields).replaceAll("\n", "\n|  ") }
             |}
            """.stripMargin
      }

      // Convert all services in the universe to Thrift services.
      val services = universe.services.values collect {
        case Service(name, functions) =>
          s"""service $name {
             |  ${ functions.map(GenThrift.serialize).mkString("\n").replaceAll("\n", "\n|  ") }
             |}
           """.stripMargin
      }

      // Generate a Thrift IDL.
      s"""namespace * $namespace
         |
         |${ structs.mkString("\n") }
         |${ services.mkString("\n") }
       """.stripMargin
    }
  }

  /**
   * Returns a Thrift-compatible representation of the [[Alias]].
   *
   * @param alias [[Alias]] to serialize.
   * @return Serialized representation.
   */
  def serialize(alias: Alias): String = alias match {
    case Alias(_, Null)       => "void"
    case Alias(_, String)     => "string"
    case Alias(_, Double)     => "double"
    case Alias(_, Int)        => "i64"
    case Alias(_, Boolean)    => "bool"
    case Alias(_, Pointer(_)) => "string"
    case Alias(n, Record(_))  => n
  }

  /**
   * Returns a Thrift-compatible representation of the list of parameters.
   *
   * @param params Parameters to serialize.
   * @return Serialized representation.
   */
  def serialize(params: Iterable[(String, Alias)]): String =
    params.zipWithIndex map { case ((n, a), i) => s"${ i+1 }: ${ serialize(a) } $n;"} mkString "\n"

  /**
   * Returns a Thrift-compatible representation of the [[Function]].
   *
   * @param function [[Function]]  to serialize.
   * @return Serialized representation.
   */
  def serialize(function: Function): String =
    s"""${ serialize(function.returns) } ${function.name}(
       |  ${ serialize(function.args.map(a => (a.name, a.datatype))).replaceAll("\n", "\n|  ") }
       |);
     """.stripMargin


}