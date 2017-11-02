//package caustic.compiler
//
//import org.antlr.v4.runtime.tree.TerminalNode
//
///**
// * A compiler error.
// *
// * @param code Unique identifier.
// * @param name Title.
// * @param description Message text.
// */
//case class Error(node: TerminalNode, code: )
//
//
//
//case class Error(code: Int, name: String, description: String) extends Exception
//
//
//
//object Error {
//
//  /**
//   *
//   * @param description
//   * @return
//   */
//  def syntax(description: String): Error = Error(0, "Syntax Error", description)
//
//  /**
//   *
//   * @param description
//   * @return
//   */
//  def typing(description: String): Error = Error(1, "Type Mismatch", description)
//
//}