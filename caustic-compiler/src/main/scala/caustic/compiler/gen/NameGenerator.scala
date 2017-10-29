package caustic.compiler.gen

import caustic.grammar._
import caustic.compiler.typing._

import scala.collection.JavaConverters._

/**
 *
 * @param universe
 */
case class NameGenerator(
  universe: Universe
) extends CausticBaseVisitor[Result] {

  override def visitName(ctx: CausticParser.NameContext): Result = {
    // Return a value that corresponds to the variable name or the key that contain the identifier.
    ctx.Identifier().asScala.drop(1).map(_.getText).foldLeft {
      this.universe.getVariable(ctx.Identifier(0).getText) match {
        case Variable(x: Primitive, n) => Result(x, s"""text("$n")""")
        case Variable(x: Pointer, n) => Result(x, s"""load("$n")""")
        case Variable(x: Record, n) => Result(x, s"""text("$n")""")
      }
    } { (key, field) =>
      // Determine the type of the field.
      val symbol = key.tag match {
        case Pointer(x: Record) => x.fields(field)
        case Record(f) => f(field)
      }

      (key.tag, symbol) match {
        case (_: Pointer, x: Pointer) =>
          // Automatically dereference nested pointers.
          Result(x, s"""read(add(${ key.value }, text("@$field")))""")
        case (_: Pointer, x: Record) =>
          // Concatenate the field name with the key.
          Result(Pointer(x), s"""add(${ key.value }, text("@$field"))""")
        case (_: Pointer, x: Primitive) =>
          // Automatically dereference primitive pointers.
          Result(Pointer(x), s"""add(${ key.value }, text("@$field"))""")
        case (_: Record, x: Pointer) =>
          // Load the pointer to the field.
          Result(x, s"""load(add(${ key.value }, text("@$field")))""")
        case (_: Record, x: Record) =>
          // Concatenate the field name with the key.
          Result(x, s"""add(${ key.value }, text("@$field"))""")
        case (_: Record, x: Primitive) =>
          // Load the primitive field.
          Result(x, s"""add(${ key.value }, text("@$field"))""")
      }
    }
  }

}
