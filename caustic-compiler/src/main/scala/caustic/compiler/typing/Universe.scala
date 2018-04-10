package caustic.compiler.typing

import caustic.compiler.Error
import caustic.grammar.CausticParser

import scala.collection.mutable

/**
 * A collection of bindings.
 *
 * @param parent
 * @param bindings
 */
class Universe(parent: Option[Universe], bindings: mutable.Map[String, Binding]) {

  /**
   * Returns the binding with the specified name.
   *
   * @param name
   * @return
   */
  def find(name: String): Option[Binding] =
    this.bindings.get(name).orElse(this.parent.flatMap(_.find(name)))

  /**
   * Returns the kind corresponding to the specified context.
   *
   * @param context ANTLR parse context.
   * @return Kind.
   */
  def kind(context: CausticParser.TypeContext): Kind = find(context.Identifier().getText) match {
    case Some(k: Struct) if context.Ampersand() != null => Pointer(k)
    case Some(k: Struct) => k
    case Some(k: Primitive) => k
    case _ => throw Error.Type(s"Unknown type ${ context.getText }", Error.Trace(context))
  }

  /**
   * Binds the binding to the specified name.
   *
   * @param name Name.
   * @param binding Binding.
   */
  def bind(name: String, binding: Binding): Unit =
    this.bindings += name -> binding

  /**
   * Returns a child universe with this universe as their parent.
   *
   * @return Child universe.
   */
  def child: Universe = new Universe(Some(this), mutable.Map.empty)

}

object Universe {

  /**
   * Constructs a universe with the default initial bindings.
   *
   * @return Root universe.
   */
  def root: Universe = new Universe(None, mutable.Map(
    "Unit" -> CUnit,
    "Boolean" -> CBoolean,
    "Int" -> CInt,
    "Double" -> CDouble,
    "String" -> CString
  ))

}