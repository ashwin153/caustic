package caustic.syntax
package http

/**
 * A compiler that generates a restful HTTP interface. For some foo.acid, the HttpCompiler generates
 * an executable HttpServer that serves a route for each function. Functions with only primitive or
 * reference arguments are generated as GET methods and all others are generated as POST methods.
 * The HttpCompiler gives Caustic out-of-box interoperability with browsers and shells.
 */
class HttpCompiler extends Compiler {

}

object HttpCompiler {


}