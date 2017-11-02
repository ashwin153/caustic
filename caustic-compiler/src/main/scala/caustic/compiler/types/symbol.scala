package caustic.compiler.types

/**
 * An entry in the [[Universe]].
 */
sealed trait Symbol

/**
 * A local variable in a [[caustic.runtime.thrift.Transaction]]. Variables are the targets of
 * [[caustic.runtime.thrift.Store]] and [[caustic.runtime.thrift.Load]] expressions. The name
 * parameter corresponds to the name of the variable within the program, and the key parameter
 * corresponds to the globally unique identifier of the variable within the runtime.
 *
 * @param name Local name.
 * @param key Runtime identifier.
 * @param datatype Underlying [[Type]].
 */
case class Variable(
  name: String,
  key: String,
  datatype: Type
) extends Symbol

/**
 * A [[Type]] alias.
 *
 * @param name Name.
 * @param datatype Underlying [[Type]].
 */
case class Alias(
  name: String,
  datatype: Type
) extends Symbol

/**
 * A [[Function]] argument. An argument is similar to a [[Variable]], except that it retains
 * additional information about the name of the underlying [[Type]] that is required to build a
 * correct [[Function]] signature.
 *
 * @param name Local name.
 * @param key Runtime identifier.
 * @param alias Underlying type [[Alias]].
 */
case class Argument(
  name: String,
  key: String,
  alias: Alias
) extends Symbol

/**
 * A transformation. Functions contain a sequence of [[Argument]] arguments, whose value must set
 * before evaluating the body of the function.
 *
 * @param name Name.
 * @param args Arguments.
 * @param returns Return type.
 * @param body Contents.
 */
case class Function(
  name: String,
  args: Seq[Argument],
  returns: Alias,
  body: Result
) extends Symbol

/**
 * A collection of related [[Function]] symbols.
 *
 * @param name Name.
 * @param functions Functions.
 */
case class Service(
  name: String,
  functions: Seq[Function]
) extends Symbol