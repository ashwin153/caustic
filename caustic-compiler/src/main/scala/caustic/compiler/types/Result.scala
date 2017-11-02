package caustic.compiler.types

/**
 * A strongly-typed value. Result values contain a [[caustic.runtime.thrift.Transaction]] when
 * executed, and their tags correspond to the static [[Type]] information known about the value by
 * the compiler. Results are used by the compiler to enforce strong typing.
 *
 * @param tag [[Type]] tag.
 * @param value Contents.
 */
case class Result(tag: Type, value: String)
