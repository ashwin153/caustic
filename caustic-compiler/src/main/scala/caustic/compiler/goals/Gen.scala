package caustic.compiler.goals

import caustic.compiler.Goal
import caustic.compiler.types._
import caustic.grammar.CausticParser

import scala.util.Try

/**
 *
 */
object Gen extends Goal[String] {

  override def execute(parser: CausticParser): Try[String] = {
    // Determine the Scala package of the program.
    val program = parser.program()
    val namespace = if (program.Module() != null) program.module(0).getText else ""

    Try(Declare(Universe.root).visitProgram(program)) map { universe =>
      // Convert Caustic records to Scala case classes.
      val records = universe.aliases.values collect {
        case Alias(name, record: Record) =>
          s"""case class $name(
             |  ${ record.fields map { case (n, t) => s"$n: ${ toScala(t) }" } mkString ",\n|  " }
             |)
             |
             |object $name$$Protocol extends DefaultJsonProtocol {
             |  implicit val ${ name }Format = jsonFormat${ record.fields.size }($name)
             |}
             |
             |import $name$$Protocol._
           """.stripMargin
      }

      // Convert Caustic services to Scala case classes.
      val services = universe.services.values collect {
        case Service(name, functions) =>
          s"""case class $name(
             |  client: Client
             |) {
             |  ${ functions.map(toFunction).mkString("\n").replaceAll("\n", "\n|  ") }
             |}
           """.stripMargin
      }

      // Generate a Thrift IDL.
      s"""package $namespace
         |
         |import caustic.runtime.service._
         |import spray.json._
         |import scala.util.Try
         |
         |${ records.mkString("\n") }
         |${ services.mkString("\n") }
       """.stripMargin
    }
  }

  /**
   * Converts a Caustic [[Alias]] into a Scala type.
   *
   * @param alias Type [[Alias]].
   * @return Corresponding Scala type.
   */
  def toScala(alias: Alias): String = alias match {
    case Alias(_, Null)       => "Unit"
    case Alias(_, String)     => "String"
    case Alias(_, Double)     => "Double"
    case Alias(_, Int)        => "Int"
    case Alias(_, Boolean)    => "Boolean"
    case Alias(_, Pointer(_)) => "String"
    case Alias(n, Record(_))  => n
  }

  /**
   * Converts a Caustic [[Function]] to a Scala function.
   *
   * @param function [[Function]] declaration.
   * @return Scala function.
   */
  def toFunction(function: Function): String = {
    // Pass Scala arguments to the underlying Caustic function.
    val pass = function.args
      .map(x => toCaustic(x.name, Result(x.alias.datatype, x.key)))
      .fold("Empty")((a, b) => s"cons($a, $b)")

    // Construct a Scala function.
    s"""def ${ function.name }(
       |  ${ function.args.map(x => s"""${ x.name }: ${ toScala(x.alias) }""").mkString(",\n|  ") }
       |): Try[${ toScala(function.returns) }] = {
       |  this.client.execute(cons(
       |    $pass,
       |    ${ toJson(function.body) }
       |  )) map { result =>
       |    if (result.isSetText)
       |      result.getText
       |    else if (result.isSetReal)
       |      result.getReal.toString
       |    else if (result.isSetFlag)
       |      result.getFlag.toString
       |    else
       |      "null"
       |  } map {
       |    _.parseJson.convertTo[${ toScala(function.returns) }]
       |  }
       |}
     """.stripMargin
  }

  /**
   * Converts a Scala variable into a Caustic [[Result]].
   *
   * @param variable Scala variable name.
   * @param result Result location.
   * @return Copy [[caustic.runtime.thrift.Transaction]].
   */
  def toCaustic(variable: String, result: Result): String = result.tag match {
    case String =>
      s"""store(${ result.value }, text($variable))"""
    case Double =>
      s"""store(${ result.value }, real($variable))"""
    case Int =>
      s"""store(${ result.value }, real($variable))"""
    case Boolean =>
      s"""store(${ result.value }, flag($variable))"""
    case Pointer(_) =>
      s"""store(${ result.value }, text($variable))"""
    case Record(fields) =>
      fields.zip(Simplify.fields(result)).map {
        case (f, r) => toCaustic(s"""$variable.$f""", r)
      }.foldLeft("Empty")((a, b) => s"""cons($a, $b)""")
  }

  /**
   * Serializes a Caustic [[Result]] to JSON.
   *
   * @param result Caustic [[Result]].
   * @return Serialized representation.
   */
  def toJson(result: Result): String = result.tag match {
    case Record(fields) =>
      // Serialize the fields of the record.
      val json = fields.keys.zip(Simplify.fields(result)) map {
        case (n, f) => s"""add(text("\\"$n\\":"), ${ toJson(f) })"""
      } reduce((a, b) => s"""add(add($a, text(",")), $b)""")

      // Serialize records as json objects.
      s"""add(add(text("{ "), $json), text(" }"))"""
    case String =>
      // Serialize string and pointer fields as quoted values.
      s"""add(add(text("\\""), load(${ result.value })), text("\\""))"""
    case Pointer(_) =>
      // Serialize string and pointer fields as quoted values.
      s"""add(add(text("\\""), ${ result.value }), text("\\""))"""
    case _ =>
      // Serialize all other types normally.
      s"""load(${ result.value })"""
  }

}
