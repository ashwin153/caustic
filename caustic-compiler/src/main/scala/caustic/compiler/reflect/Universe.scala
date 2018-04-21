package caustic.compiler.reflect

import scala.collection.mutable

/**
 * A collection of bindings.
 *
 * @param parent Parent universe.
 * @param bindings Known bindings.
 */
class Universe(parent: Option[Universe], bindings: mutable.Map[String, Binding]) {

  /**
   * Returns the binding with the specified name.
   *
   * @param name Name of binding.
   * @return Corresponding binding.
   */
  def find(name: String): Option[Binding] =
    this.bindings.get(name).orElse(this.parent.flatMap(_.find(name)))

  /**
   * Binds the binding to the specified name.
   *
   * @param name Name.
   * @param binding Binding.
   * @return Universe.
   */
  def bind(name: String, binding: Binding): Universe = {
    this.bindings += name -> binding
    this
  }

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
  def root: Universe = {
    new Universe(None, mutable.Map.empty)
      // Primitive.
      .bind("Unit", CUnit)
      .bind("Boolean", CBoolean)
      .bind("Boolean&", CPointer(CBoolean))
      .bind("Boolean&$Constructor", CFunction("Variable.Remote", List(CString), CPointer(CBoolean)))
      .bind("Int", CInt)
      .bind("Int&", CPointer(CInt))
      .bind("Int&$Constructor", CFunction("Variable.Remote", List(CString), CPointer(CInt)))
      .bind("Double", CDouble)
      .bind("Double&", CPointer(CDouble))
      .bind("Double&$Constructor", CFunction("Variable.Remote", List(CString), CPointer(CDouble)))
      .bind("String", CString)
      .bind("String&", CPointer(CString))
      .bind("String&$Constructor", CFunction("Variable.Remote", List(CString), CPointer(CString)))

      // Collections.
      .bind("List[Boolean]", CList(CBoolean))
      .bind("List[Boolean]$Constructor", CFunction("List.Local[Boolean]", List(CUnit), CList(CBoolean)))
      .bind("List[Boolean]&", CPointer(CList(CBoolean)))
      .bind("List[Boolean]&$Constructor", CFunction("List.Remote[Boolean]", List(CString), CPointer(CList(CBoolean))))
      .bind("List[Int]", CList(CInt))
      .bind("List[Int]$Constructor", CFunction("List.Local[Int]", List(CUnit), CList(CInt)))
      .bind("List[Int]&", CPointer(CList(CInt)))
      .bind("List[Int]&$Constructor", CFunction("List.Remote[Int]", List(CString), CPointer(CList(CInt))))
      .bind("List[Double]", CList(CDouble))
      .bind("List[Double]$Constructor", CFunction("List.Local[Double]", List(CUnit), CList(CDouble)))
      .bind("List[Double]&", CPointer(CList(CDouble)))
      .bind("List[Double]&$Constructor", CFunction("List.Remote[Double]", List(CString), CPointer(CList(CDouble))))
      .bind("List[String]", CList(CString))
      .bind("List[String]$Constructor", CFunction("List.Local[String]", List(CUnit), CList(CString)))
      .bind("List[String]&", CPointer(CList(CString)))
      .bind("List[String]&$Constructor", CFunction("List.Remote[String]", List(CString), CPointer(CList(CString))))
      .bind("Set[Boolean]", CSet(CBoolean))
      .bind("Set[Boolean]$Constructor", CFunction("Set.Local[Boolean]", List(CUnit), CSet(CBoolean)))
      .bind("Set[Boolean]&", CPointer(CSet(CBoolean)))
      .bind("Set[Boolean]&$Constructor", CFunction("Set.Remote[Boolean]", List(CString), CPointer(CSet(CBoolean))))
      .bind("Set[Int]", CSet(CInt))
      .bind("Set[Int]$Constructor", CFunction("Set.Local[Int]", List(CUnit), CSet(CInt)))
      .bind("Set[Int]&", CPointer(CSet(CInt)))
      .bind("Set[Int]&$Constructor", CFunction("Set.Remote[Int]", List(CString), CPointer(CSet(CInt))))
      .bind("Set[Double]", CSet(CDouble))
      .bind("Set[Double]$Constructor", CFunction("Set.Local[Double]", List(CUnit), CSet(CDouble)))
      .bind("Set[Double]&", CPointer(CSet(CDouble)))
      .bind("Set[Double]&$Constructor", CFunction("Set.Remote[Double]", List(CString), CPointer(CSet(CDouble))))
      .bind("Set[String]", CSet(CString))
      .bind("Set[String]$Constructor", CFunction("Set.Local[String]", List(CUnit), CSet(CString)))
      .bind("Set[String]&", CPointer(CSet(CString)))
      .bind("Set[String]&$Constructor", CFunction("Set.Remote[String]", List(CString), CPointer(CSet(CString))))
      .bind("Map[Boolean,Boolean]", CMap(CBoolean, CBoolean))
      .bind("Map[Boolean,Boolean]$Constructor", CFunction("Map.Local[Boolean, Boolean]", List(CUnit), CMap(CBoolean, CBoolean)))
      .bind("Map[Boolean,Boolean]&", CPointer(CMap(CBoolean, CBoolean)))
      .bind("Map[Boolean,Boolean]&$Constructor", CFunction("Map.Remote[Boolean, Boolean]", List(CString), CPointer(CMap(CBoolean, CBoolean))))
      .bind("Map[Boolean,Int]", CMap(CBoolean, CInt))
      .bind("Map[Boolean,Int]$Constructor", CFunction("Map.Local[Boolean, Int]", List(CUnit), CMap(CBoolean, CInt)))
      .bind("Map[Boolean,Int]&", CPointer(CMap(CBoolean, CInt)))
      .bind("Map[Boolean,Int]&$Constructor", CFunction("Map.Remote[Boolean, Int]", List(CString), CPointer(CMap(CBoolean, CInt))))
      .bind("Map[Boolean,Double]", CMap(CBoolean, CDouble))
      .bind("Map[Boolean,Double]$Constructor", CFunction("Map.Local[Boolean, Double]", List(CUnit), CMap(CBoolean, CDouble)))
      .bind("Map[Boolean,Double]&", CPointer(CMap(CBoolean, CDouble)))
      .bind("Map[Boolean,Double]&$Constructor", CFunction("Map.Remote[Boolean, Double]", List(CString), CPointer(CMap(CBoolean, CDouble))))
      .bind("Map[Boolean,String]", CMap(CBoolean, CString))
      .bind("Map[Boolean,String]$Constructor", CFunction("Map.Local[Boolean, String]", List(CUnit), CMap(CBoolean, CString)))
      .bind("Map[Boolean,String]&", CPointer(CMap(CBoolean, CString)))
      .bind("Map[Boolean,String]&$Constructor", CFunction("Map.Remote[Boolean, String]", List(CString), CPointer(CMap(CBoolean, CString))))
      .bind("Map[Int,Boolean]", CMap(CInt, CBoolean))
      .bind("Map[Int,Boolean]$Constructor", CFunction("Map.Local[Int, Boolean]", List(CUnit), CMap(CInt, CBoolean)))
      .bind("Map[Int,Boolean]&", CPointer(CMap(CInt, CBoolean)))
      .bind("Map[Int,Boolean]&$Constructor", CFunction("Map.Remote[Int, Boolean]", List(CString), CPointer(CMap(CInt, CBoolean))))
      .bind("Map[Int,Int]", CMap(CInt, CInt))
      .bind("Map[Int,Int]$Constructor", CFunction("Map.Local[Int, Int]", List(CUnit), CMap(CInt, CInt)))
      .bind("Map[Int,Int]&", CPointer(CMap(CInt, CInt)))
      .bind("Map[Int,Int]&$Constructor", CFunction("Map.Remote[Int, Int]", List(CString), CPointer(CMap(CInt, CInt))))
      .bind("Map[Int,Double]", CMap(CInt, CDouble))
      .bind("Map[Int,Double]$Constructor", CFunction("Map.Local[Int, Double]", List(CUnit), CMap(CInt, CDouble)))
      .bind("Map[Int,Double]&", CPointer(CMap(CInt, CDouble)))
      .bind("Map[Int,Double]&$Constructor", CFunction("Map.Remote[Int, Double]", List(CString), CPointer(CMap(CInt, CDouble))))
      .bind("Map[Int,String]", CMap(CInt, CString))
      .bind("Map[Int,String]$Constructor", CFunction("Map.Local[Int, String]", List(CUnit), CMap(CInt, CString)))
      .bind("Map[Int,String]&", CPointer(CMap(CInt, CString)))
      .bind("Map[Int,String]&$Constructor", CFunction("Map.Remote[Int, String]", List(CString), CPointer(CMap(CInt, CString))))
      .bind("Map[Double,Boolean]", CMap(CDouble, CBoolean))
      .bind("Map[Double,Boolean]$Constructor", CFunction("Map.Local[Double, Boolean]", List(CUnit), CMap(CDouble, CBoolean)))
      .bind("Map[Double,Boolean]&", CPointer(CMap(CDouble, CBoolean)))
      .bind("Map[Double,Boolean]&$Constructor", CFunction("Map.Remote[Double, Boolean]", List(CString), CPointer(CMap(CDouble, CBoolean))))
      .bind("Map[Double,Int]", CMap(CDouble, CInt))
      .bind("Map[Double,Int]$Constructor", CFunction("Map.Local[Double, Int]", List(CUnit), CMap(CDouble, CInt)))
      .bind("Map[Double,Int]&", CPointer(CMap(CDouble, CInt)))
      .bind("Map[Double,Int]&$Constructor", CFunction("Map.Remote[Double, Int]", List(CString), CPointer(CMap(CDouble, CInt))))
      .bind("Map[Double,Double]", CMap(CDouble, CDouble))
      .bind("Map[Double,Double]$Constructor", CFunction("Map.Local[Double, Double]", List(CUnit), CMap(CDouble, CDouble)))
      .bind("Map[Double,Double]&", CPointer(CMap(CDouble, CDouble)))
      .bind("Map[Double,Double]&$Constructor", CFunction("Map.Remote[Double, Double]", List(CString), CPointer(CMap(CDouble, CDouble))))
      .bind("Map[Double,String]", CMap(CDouble, CString))
      .bind("Map[Double,String]$Constructor", CFunction("Map.Local[Double, String]", List(CUnit), CMap(CDouble, CString)))
      .bind("Map[Double,String]&", CPointer(CMap(CDouble, CString)))
      .bind("Map[Double,String]&$Constructor", CFunction("Map.Remote[Double, String]", List(CString), CPointer(CMap(CDouble, CString))))
      .bind("Map[String,Boolean]", CMap(CString, CBoolean))
      .bind("Map[String,Boolean]$Constructor", CFunction("Map.Local[String, Boolean]", List(CUnit), CMap(CString, CBoolean)))
      .bind("Map[String,Boolean]&", CPointer(CMap(CString, CBoolean)))
      .bind("Map[String,Boolean]&$Constructor", CFunction("Map.Remote[String, Boolean]", List(CString), CPointer(CMap(CString, CBoolean))))
      .bind("Map[String,Int]", CMap(CString, CInt))
      .bind("Map[String,Int]$Constructor", CFunction("Map.Local[String, Int]", List(CUnit), CMap(CString, CInt)))
      .bind("Map[String,Int]&", CPointer(CMap(CString, CInt)))
      .bind("Map[String,Int]&$Constructor", CFunction("Map.Remote[String, Int]", List(CString), CPointer(CMap(CString, CInt))))
      .bind("Map[String,Double]", CMap(CString, CDouble))
      .bind("Map[String,Double]$Constructor", CFunction("Map.Local[String, Double]", List(CUnit), CMap(CString, CDouble)))
      .bind("Map[String,Double]&", CPointer(CMap(CString, CDouble)))
      .bind("Map[String,Double]&$Constructor", CFunction("Map.Remote[String, Double]", List(CString), CPointer(CMap(CString, CDouble))))
      .bind("Map[String,String]", CMap(CString, CString))
      .bind("Map[String,String]$Constructor", CFunction("Map.Local[String, String]", List(CUnit), CMap(CString, CString)))
      .bind("Map[String,String]&", CPointer(CMap(CString, CString)))
      .bind("Map[String,String]&$Constructor", CFunction("Map.Remote[String, String]", List(CString), CPointer(CMap(CString, CString))))

      // Math.
      .bind("abs", CFunction("abs", List(CDouble), CDouble))
      .bind("acos", CFunction("acos", List(CDouble), CDouble))
      .bind("acot", CFunction("acot", List(CDouble), CDouble))
      .bind("acsc", CFunction("acsc", List(CDouble), CDouble))
      .bind("asec", CFunction("asec", List(CDouble), CDouble))
      .bind("asin", CFunction("asin", List(CDouble), CDouble))
      .bind("atan", CFunction("atan", List(CDouble), CDouble))
      .bind("cbrt", CFunction("cbrt", List(CDouble), CDouble))
      .bind("ceil", CFunction("ceil", List(CDouble), CInt))
      .bind("cos", CFunction("cos", List(CDouble), CDouble))
      .bind("cosh", CFunction("cosh", List(CDouble), CDouble))
      .bind("cot", CFunction("cot", List(CDouble), CDouble))
      .bind("coth", CFunction("coth", List(CDouble), CDouble))
      .bind("csc", CFunction("csc", List(CDouble), CDouble))
      .bind("csch", CFunction("csch", List(CDouble), CDouble))
      .bind("exp", CFunction("exp", List(CDouble), CDouble))
      .bind("expm1", CFunction("expm1", List(CDouble), CDouble))
      .bind("floor", CFunction("floor", List(CDouble), CInt))
      .bind("hypot", CFunction("hypot", List(CDouble, CDouble), CDouble))
      .bind("log", CFunction("log", List(CDouble), CDouble))
      .bind("log10", CFunction("log10", List(CDouble), CDouble))
      .bind("log1p", CFunction("log1p", List(CDouble), CDouble))
      .bind("pow", CFunction("pow", List(CDouble, CDouble), CDouble))
      .bind("random", CFunction("random", List(), CDouble))
      .bind("rint", CFunction("rint", List(CDouble), CInt))
      .bind("round", CFunction("round", List(CDouble), CInt))
      .bind("sec", CFunction("sec", List(CDouble), CDouble))
      .bind("sech", CFunction("sech", List(CDouble), CDouble))
      .bind("signum", CFunction("signum", List(CDouble), CDouble))
      .bind("sin", CFunction("sin", List(CDouble), CDouble))
      .bind("sinh", CFunction("sinh", List(CDouble), CDouble))
      .bind("sqrt", CFunction("sqrt", List(CDouble), CDouble))
      .bind("tan", CFunction("tan", List(CDouble), CDouble))
      .bind("tanh", CFunction("tanh", List(CDouble), CDouble))
  }

}