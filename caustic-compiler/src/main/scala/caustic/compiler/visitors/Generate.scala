package caustic.compiler.visitors

import caustic.compiler.{Compiler, Handler, TypeError}
import caustic.compiler.types._
import caustic.grammar.{CausticBaseVisitor, CausticParser}
import java.nio.file.Paths
import org.antlr.v4.runtime.misc.ParseCancellationException
import scala.collection.JavaConverters._

/**
 * A code generator. Compiles a Caustic program into a Scala library. Generated code depends on
 * Spray Json (version 1.3.3) and on various components of the Caustic runtime.
 *
 * @param handler  Exception [[Handler]].
 * @param universe Known [[Universe]].
 */
case class Generate(
  handler: Handler,
  universe: Universe
) extends CausticBaseVisitor[String] {

  override def visitInclude(ctx: CausticParser.IncludeContext): String = {
    // Extract the location of the referenced file.
    val file = ctx.String().getText.substring(1, ctx.String().getText.length - 1)

    // Compile the specified source.
    val source = handler.source.getParent.resolve(file)
    val imported = Compiler.execute(source).get

    // Copy all services in the file into the context.
    imported.services.values.foreach(s => universe.putService(s.name, s))

    // Copy all records in the file into the context, and import them in the generated sources.
    imported.aliases.values collect {
      case Alias(n, r: Record) =>
        universe.putAlias(n, r)
        s"""import ${ imported.labels.drop(1) mkString "." }._"""
    } mkString "\n"
  }

  override def visitRecord(ctx: CausticParser.RecordContext): String = {
    val fields = ctx.parameters().parameter().asScala map { param =>
      // Extract the name of the field and its type.
      val ftype = param.`type`().getText
      val fname = param.Identifier().getText

      // Construct the field, or throw an exception if the type doesn't exist.
      fname -> this.universe.getAlias(ftype).getOrElse {
        this.handler.report(TypeError, s"Undefined type $ftype.", param.`type`().Identifier())
        throw new ParseCancellationException()
      }
    }

    val parent = Option(ctx.Extends()) match {
      case Some(_) =>
        // Merge the fields of the superclass with that of the record.
        this.universe.getAlias(ctx.Identifier(1).getText) match {
          case Some(Alias(_, Record(inherited))) =>
            inherited
          case Some(Alias(_, t)) =>
            handler.report(TypeError, s"${ ctx.Identifier(1).getText } is of type $t not Record.", ctx.Identifier(1))
            throw new ParseCancellationException()
          case None =>
            handler.report(TypeError, s"${ ctx.Identifier(1).getText } is not defined", ctx.Identifier(1))
            throw new ParseCancellationException()
        }
      case None =>
        // Otherwise, set the inherited fields to empty.
        Map.empty[String, Alias]
    }

    // Extract the record name, and add the record to the universe.
    val record = ctx.Identifier(0).getText
    this.universe.putAlias(record, Record(parent ++ fields))

    // Serialize the field parameters.
    val params = (parent ++ fields) mapValues toScala
    val define = params map {
      case (n, t) if parent.contains(n) => s"override val $n: $t"
      case (n, t) => s"val $n: $t"
    }

    // Construct the superclass declaration.
    val inherits = Option(ctx.Extends()) match {
      case Some(_) =>
        s"extends ${ ctx.Identifier(1).getText }(${parent.keys mkString "," }) with Product"
      case None =>
        "extends Product"
    }

    // Serialize Caustic records as Scala classes.
    s"""// TODO: Copy block comment from *.acid file.
       |class $record(
       |  ${ define mkString ",\n|  " }
       |) $inherits {
       |  override def canEqual(that: Any): Boolean = that match {
       |    case x: $record =>
       |      ${ params.keys map { f => s"this.$f == x.$f" } mkString " && " }
       |    case _ =>
       |      false
       |  }
       |
       |  override def productArity: Int = ${ params.size }
       |
       |  override def productElement(n: Int): Any =
       |    ${ params.keys.zipWithIndex map { case (f, i) => s"if (n == $i) $f" } mkString " else " }
       |
       |  override def toString: String =
       |    "$record(" + this.productIterator.map(_.toString).mkString(", ") + ")"
       |}
       |
       |/**
       | * A Spray Json serialization protocol for instances of [[$record]].
       | */
       |object $record {
       |  implicit object ${ record }Format extends RootJsonFormat[$record] {
       |    def write(x: $record): JsValue = JsObject(
       |      ${ params.keys.map(f => s""""$f" -> x.$f.toJson""") mkString ", " }
       |    )
       |
       |    def read(x: JsValue): $record = {
       |      x.asJsObject.getFields(${ params.keys.map(f => s""""$f"""") mkString ", " }) match {
       |        case Seq(${ params.keys mkString "," }) =>
       |          $record(${ params map { case (n, t) => s"$n.convertTo[$t]" } mkString ", " })
       |        case _ => throw DeserializationException("$record expected, but not found.")
       |      }
       |    }
       |  }
       |
       |  def apply(${ params map { case (n, t) => s"$n: $t" } mkString ", " }): $record =
       |    new $record(${ params.keys mkString ", " })
       |}
       |
       |import $record._
     """.stripMargin
  }

  override def visitService(ctx: CausticParser.ServiceContext): String = {
    // Extract the service name, and construct a child universe for its functions.
    val service = ctx.Identifier(0).getText
    val context = this.universe.child(service)

    var functions = ctx.function().asScala map { function =>
      val args = function.parameters().parameter().asScala map { param =>
        // Extract the name of the argument and type.
        val atype = param.`type`().getText
        val aname = param.Identifier().getText

        // Construct the argument, or throw an exception if the type doesn't exist.
        aname -> this.universe.getAlias(atype).getOrElse {
          this.handler.report(TypeError, s"Undefined type $atype.", param.`type`().Identifier())
          throw new ParseCancellationException()
        }
      }

      // Extract the name of the function and return type.
      val fname = function.Identifier().getText
      val ftype = function.`type`().getText

      // Construct the return type, or throw an exception if the return type doesn't exist.
      val returns = this.universe.getAlias(ftype).getOrElse {
        this.handler.report(TypeError, s"Undefined type $ftype.", function.`type`().Identifier())
        throw new ParseCancellationException()
      }

      // Add the function to the universe, and return the constructed function.
      context.putFunction(fname, args.toMap, returns)(Simplify(this.handler, _).visitBlock(function.block()))
      context.getFunction(fname).get
    }

    if (ctx.Extends() != null) {
      // Extract the name of the superclass.
      val parent = ctx.Identifier(1).getText

      // Merge the fields of the superclass with that of the record.
      this.universe.getService(parent) match {
        case Some(Service(_, inherited)) =>
          functions = inherited.toBuffer ++ functions
        case None =>
          handler.report(TypeError, s"$parent is not defined", ctx.Identifier(1))
      }
    }

    // Add the service to the universe.
    this.universe.putService(service, Service(service, functions))

    // Serialize Caustic services as Scala case classes.
    s"""// TODO: Copy block comment from *.acid file.
       |case class $service(client: Client) {
       |  ${ functions.map(toFunction).mkString("\n").replaceAll("\n", "\n|  ") }
       |}
     """.stripMargin
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
    case Alias(n, Pointer(_)) => s"Pointer[$n]"
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
    val body = function.args
      .map(x => toCaustic(x.name, Result(x.alias.datatype, x.key)))
      .foldRight(toJson(function.body))((a, b) => s"cons($a, $b)")

    // Construct a Scala function.
    s"""// TODO: Copy block comment from *.acid file.
       |def ${ function.name }(
       |  ${ function.args.map(x => s"""${ x.name }: ${ toScala(x.alias) }""").mkString(",\n|  ") }
       |): Try[${ toScala(function.returns) }] = {
       |  this.client.execute($body) map { result =>
       |    // Extract a Json string from the result.
       |    if (result.isSetText)
       |      result.getText
       |    else if (result.isSetReal)
       |      result.getReal.toString
       |    else if (result.isSetFlag)
       |      result.getFlag.toString
       |    else
       |      ""
       |  } map {
       |    // Deserialize the result using Spray Json.
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
   * @return Copy [[Result]] to [[Variable]].
   */
  def toCaustic(variable: String, result: Result): String = result.tag match {
    case Null =>
      s"""store(${ result.value }, None)"""
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
        case (f, r) => toCaustic(s"""$variable.${ f._1 }""", r)
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
    case Pointer(_) =>
      // Serialize string and pointer fields as quoted values.
      s"""add(add(text("\\""), ${ result.value }), text("\\""))"""
    case String =>
      // Serialize string and pointer fields as quoted values.
      s"""add(add(text("\\""), load(${ result.value })), text("\\""))"""
    case _ =>
      // Serialize all other types normally.
      result.value
  }

}
