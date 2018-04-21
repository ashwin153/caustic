package caustic.compiler.util

import scala.language.implicitConversions

/**
 *
 * @param context
 */
class Indenter(context: StringContext) {

  def i(args: Any*): String = {
    val builder = new StringBuilder()
    (context.parts zip args) foreach { case (str, arg) =>
      builder.append(str.replaceAll("\\s*\\|", "\n"))
      val whitespace = builder.substring(builder.lastIndexOf("\n") + 1)
      val indent = if (whitespace.trim.isEmpty) whitespace.length else 0
      builder.append(arg.toString.replaceAll("\n", whitespace + "\n" + " " * indent))
    }

    if (context.parts.size > args.size)
      builder.append(context.parts.last.replaceAll("\\s*\\|", "\n"))

    builder.toString
  }

}