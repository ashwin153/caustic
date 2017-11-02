package caustic.compiler.goals

import caustic.compiler.{Goal, types, types}
import caustic.compiler.types._
import caustic.compiler.types._
import caustic.grammar.{CausticBaseVisitor, CausticParser}
import scala.collection.JavaConverters._
import scala.util.Try

/**
 *
 * @param universe
 */
case class Gen(universe: Universe) extends Goal[String] {

  override def execute(parser: CausticParser): Try[String] =
    Declare(universe).execute(parser) map { universe =>
      // Convert all declarations in the universe to Thrift.
      universe.symbols.values collect {
        case Alias(name, r: Record) => Gen.serialize(name, r)
        case s: Service => Gen.serialize(s)
      } mkString "\n"
    }

}

object Gen {

  /**
   *
   * @param alias
   * @return
   */
  def serialize(alias: Alias): String = alias match {
    case Alias(_, types.Null)    => "void"
    case Alias(_, types.String)  => "string"
    case Alias(_, types.Double)  => "double"
    case Alias(_, types.Int)     => "i64"
    case Alias(_, types.Boolean) => "bool"
    case Alias(_, Pointer(_))    => "string"
    case Alias(n, Record(_))     => n
  }

  /**
   *
   * @param params
   * @return
   */
  def serialize(params: Iterable[(String, Alias)]): String =
    params.zipWithIndex map {
      case ((name, alias), i) => s"${ i + 1 }: ${ serialize(alias) } $name;"
    } mkString "\n"

  /**
   *
   * @param record
   * @return
   */
  def serialize(name: String, record: Record): String =
    s"""
       |struct $name {
       |  ${ serialize(record.fields).replaceAll("\n", "\n|  ") }
       |}
     """.stripMargin

  /**
   *
   * @param function
   * @return
   */
  def serialize(function: types.Function): String =
    s"""
       |${ serialize(function.returns) } ${function.name}(
       |  ${ serialize(function.args.map(a => (a.name, a.datatype))).replaceAll("\n", "\n|  ") }
       |)
     """.stripMargin

  /**
   *
   * @param service
   * @return
   */
  def serialize(service: types.Service): String =
    s"""
       |service ${ service.name } {
       |  ${ service.functions.map(serialize).mkString("\n").replaceAll("\n", "\n|  ") }
       |}
     """.stripMargin

}