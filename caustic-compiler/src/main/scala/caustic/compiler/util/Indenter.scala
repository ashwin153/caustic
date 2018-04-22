package caustic.compiler.util

/**
 * An indentation preserving string context. Standard string interpolation does not preserve
 * indentation; s"  $x" will indent only the first line of x and not subsequent lines. Indenters
 * apply the same indentation to all lines of x; i"  $x" will indent all lines of x by 2.
 *
 * @see https://stackoverflow.com/a/11426477/1447029
 * @param context String context.
 */
case class Indenter(context: StringContext) {

  def i(args: Any*): String = {
    val builder = new StringBuilder()
    val parts = context.parts.map(_.stripMargin)

    (parts zip args) foreach { case (part, arg) =>
      builder.append(part)
      val whitespace = builder.substring(builder.lastIndexOf("\n") + 1)
      val indent = if (whitespace.trim.isEmpty) whitespace.length else 0
      builder.append(arg.toString.replaceAll("\n(?!$)", "\n" + " " * indent))
    }

    if (parts.size > args.size) builder.append(parts.last).toString else builder.toString
  }

}