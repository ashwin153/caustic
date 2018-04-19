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
      .bind("Boolean&", Pointer(CBoolean))
      .bind("Boolean&$Constructor", Function("Variable.Remote", List(CString), Pointer(CBoolean)))
      .bind("Int", CInt)
      .bind("Int&", Pointer(CInt))
      .bind("Int&$Constructor", Function("Variable.Remote", List(CString), Pointer(CInt)))
      .bind("Double", CDouble)
      .bind("Double&", Pointer(CDouble))
      .bind("Double&$Constructor", Function("Variable.Remote", List(CString), Pointer(CDouble)))
      .bind("String", CString)
      .bind("String&", Pointer(CString))
      .bind("String&$Constructor", Function("Variable.Remote", List(CString), Pointer(CString)))

      // Collections.
      .bind("List[Boolean]", CList(CBoolean))
      .bind("List[Boolean]$$Constructor", Function("List.Local[Boolean]", List(CUnit), CList(CBoolean)))
      .bind("List[Boolean]&", Pointer(CList(CBoolean)))
      .bind("List[Boolean]&$$Constructor", Function("List.Remote[Boolean]", List(CString), Pointer(CList(CBoolean))))
      .bind("List[Int]", CList(CInt))
      .bind("List[Int]$$Constructor", Function("List.Local[Int]", List(CUnit), CList(CInt)))
      .bind("List[Int]&", Pointer(CList(CInt)))
      .bind("List[Int]&$$Constructor", Function("List.Remote[Int]", List(CString), Pointer(CList(CInt))))
      .bind("List[Double]", CList(CDouble))
      .bind("List[Double]$$Constructor", Function("List.Local[Double]", List(CUnit), CList(CDouble)))
      .bind("List[Double]&", Pointer(CList(CDouble)))
      .bind("List[Double]&$$Constructor", Function("List.Remote[Double]", List(CString), Pointer(CList(CDouble))))
      .bind("List[String]", CList(CString))
      .bind("List[String]$$Constructor", Function("List.Local[String]", List(CUnit), CList(CString)))
      .bind("List[String]&", Pointer(CList(CString)))
      .bind("List[String]&$$Constructor", Function("List.Remote[String]", List(CString), Pointer(CList(CString))))
      .bind("Set[Boolean]", CSet(CBoolean))
      .bind("Set[Boolean]$$Constructor", Function("Set.Local[Boolean]", List(CUnit), CSet(CBoolean)))
      .bind("Set[Boolean]&", Pointer(CSet(CBoolean)))
      .bind("Set[Boolean]&$$Constructor", Function("Set.Remote[Boolean]", List(CString), Pointer(CSet(CBoolean))))
      .bind("Set[Int]", CSet(CInt))
      .bind("Set[Int]$$Constructor", Function("Set.Local[Int]", List(CUnit), CSet(CInt)))
      .bind("Set[Int]&", Pointer(CSet(CInt)))
      .bind("Set[Int]&$$Constructor", Function("Set.Remote[Int]", List(CString), Pointer(CSet(CInt))))
      .bind("Set[Double]", CSet(CDouble))
      .bind("Set[Double]$$Constructor", Function("Set.Local[Double]", List(CUnit), CSet(CDouble)))
      .bind("Set[Double]&", Pointer(CSet(CDouble)))
      .bind("Set[Double]&$$Constructor", Function("Set.Remote[Double]", List(CString), Pointer(CSet(CDouble))))
      .bind("Set[String]", CSet(CString))
      .bind("Set[String]$$Constructor", Function("Set.Local[String]", List(CUnit), CSet(CString)))
      .bind("Set[String]&", Pointer(CSet(CString)))
      .bind("Set[String]&$$Constructor", Function("Set.Remote[String]", List(CString), Pointer(CSet(CString))))
      .bind("Map[Boolean,Boolean]", CMap(CBoolean, CBoolean))
      .bind("Map[Boolean,Boolean]$$Constructor", Function("Map.Local[Boolean, Boolean]", List(CUnit), CMap(CBoolean, CBoolean)))
      .bind("Map[Boolean,Boolean]&", Pointer(CMap(CBoolean, CBoolean)))
      .bind("Map[Boolean,Boolean]&$$Constructor", Function("Map.Remote[Boolean, Boolean]", List(CString), Pointer(CMap(CBoolean, CBoolean))))
      .bind("Map[Boolean,Int]", CMap(CBoolean, CInt))
      .bind("Map[Boolean,Int]$$Constructor", Function("Map.Local[Boolean, Int]", List(CUnit), CMap(CBoolean, CInt)))
      .bind("Map[Boolean,Int]&", Pointer(CMap(CBoolean, CInt)))
      .bind("Map[Boolean,Int]&$$Constructor", Function("Map.Remote[Boolean, Int]", List(CString), Pointer(CMap(CBoolean, CInt))))
      .bind("Map[Boolean,Double]", CMap(CBoolean, CDouble))
      .bind("Map[Boolean,Double]$$Constructor", Function("Map.Local[Boolean, Double]", List(CUnit), CMap(CBoolean, CDouble)))
      .bind("Map[Boolean,Double]&", Pointer(CMap(CBoolean, CDouble)))
      .bind("Map[Boolean,Double]&$$Constructor", Function("Map.Remote[Boolean, Double]", List(CString), Pointer(CMap(CBoolean, CDouble))))
      .bind("Map[Boolean,String]", CMap(CBoolean, CString))
      .bind("Map[Boolean,String]$$Constructor", Function("Map.Local[Boolean, String]", List(CUnit), CMap(CBoolean, CString)))
      .bind("Map[Boolean,String]&", Pointer(CMap(CBoolean, CString)))
      .bind("Map[Boolean,String]&$$Constructor", Function("Map.Remote[Boolean, String]", List(CString), Pointer(CMap(CBoolean, CString))))
      .bind("Map[Int,Boolean]", CMap(CInt, CBoolean))
      .bind("Map[Int,Boolean]$$Constructor", Function("Map.Local[Int, Boolean]", List(CUnit), CMap(CInt, CBoolean)))
      .bind("Map[Int,Boolean]&", Pointer(CMap(CInt, CBoolean)))
      .bind("Map[Int,Boolean]&$$Constructor", Function("Map.Remote[Int, Boolean]", List(CString), Pointer(CMap(CInt, CBoolean))))
      .bind("Map[Int,Int]", CMap(CInt, CInt))
      .bind("Map[Int,Int]$$Constructor", Function("Map.Local[Int, Int]", List(CUnit), CMap(CInt, CInt)))
      .bind("Map[Int,Int]&", Pointer(CMap(CInt, CInt)))
      .bind("Map[Int,Int]&$$Constructor", Function("Map.Remote[Int, Int]", List(CString), Pointer(CMap(CInt, CInt))))
      .bind("Map[Int,Double]", CMap(CInt, CDouble))
      .bind("Map[Int,Double]$$Constructor", Function("Map.Local[Int, Double]", List(CUnit), CMap(CInt, CDouble)))
      .bind("Map[Int,Double]&", Pointer(CMap(CInt, CDouble)))
      .bind("Map[Int,Double]&$$Constructor", Function("Map.Remote[Int, Double]", List(CString), Pointer(CMap(CInt, CDouble))))
      .bind("Map[Int,String]", CMap(CInt, CString))
      .bind("Map[Int,String]$$Constructor", Function("Map.Local[Int, String]", List(CUnit), CMap(CInt, CString)))
      .bind("Map[Int,String]&", Pointer(CMap(CInt, CString)))
      .bind("Map[Int,String]&$$Constructor", Function("Map.Remote[Int, String]", List(CString), Pointer(CMap(CInt, CString))))
      .bind("Map[Double,Boolean]", CMap(CDouble, CBoolean))
      .bind("Map[Double,Boolean]$$Constructor", Function("Map.Local[Double, Boolean]", List(CUnit), CMap(CDouble, CBoolean)))
      .bind("Map[Double,Boolean]&", Pointer(CMap(CDouble, CBoolean)))
      .bind("Map[Double,Boolean]&$$Constructor", Function("Map.Remote[Double, Boolean]", List(CString), Pointer(CMap(CDouble, CBoolean))))
      .bind("Map[Double,Int]", CMap(CDouble, CInt))
      .bind("Map[Double,Int]$$Constructor", Function("Map.Local[Double, Int]", List(CUnit), CMap(CDouble, CInt)))
      .bind("Map[Double,Int]&", Pointer(CMap(CDouble, CInt)))
      .bind("Map[Double,Int]&$$Constructor", Function("Map.Remote[Double, Int]", List(CString), Pointer(CMap(CDouble, CInt))))
      .bind("Map[Double,Double]", CMap(CDouble, CDouble))
      .bind("Map[Double,Double]$$Constructor", Function("Map.Local[Double, Double]", List(CUnit), CMap(CDouble, CDouble)))
      .bind("Map[Double,Double]&", Pointer(CMap(CDouble, CDouble)))
      .bind("Map[Double,Double]&$$Constructor", Function("Map.Remote[Double, Double]", List(CString), Pointer(CMap(CDouble, CDouble))))
      .bind("Map[Double,String]", CMap(CDouble, CString))
      .bind("Map[Double,String]$$Constructor", Function("Map.Local[Double, String]", List(CUnit), CMap(CDouble, CString)))
      .bind("Map[Double,String]&", Pointer(CMap(CDouble, CString)))
      .bind("Map[Double,String]&$$Constructor", Function("Map.Remote[Double, String]", List(CString), Pointer(CMap(CDouble, CString))))
      .bind("Map[String,Boolean]", CMap(CString, CBoolean))
      .bind("Map[String,Boolean]$$Constructor", Function("Map.Local[String, Boolean]", List(CUnit), CMap(CString, CBoolean)))
      .bind("Map[String,Boolean]&", Pointer(CMap(CString, CBoolean)))
      .bind("Map[String,Boolean]&$$Constructor", Function("Map.Remote[String, Boolean]", List(CString), Pointer(CMap(CString, CBoolean))))
      .bind("Map[String,Int]", CMap(CString, CInt))
      .bind("Map[String,Int]$$Constructor", Function("Map.Local[String, Int]", List(CUnit), CMap(CString, CInt)))
      .bind("Map[String,Int]&", Pointer(CMap(CString, CInt)))
      .bind("Map[String,Int]&$$Constructor", Function("Map.Remote[String, Int]", List(CString), Pointer(CMap(CString, CInt))))
      .bind("Map[String,Double]", CMap(CString, CDouble))
      .bind("Map[String,Double]$$Constructor", Function("Map.Local[String, Double]", List(CUnit), CMap(CString, CDouble)))
      .bind("Map[String,Double]&", Pointer(CMap(CString, CDouble)))
      .bind("Map[String,Double]&$$Constructor", Function("Map.Remote[String, Double]", List(CString), Pointer(CMap(CString, CDouble))))
      .bind("Map[String,String]", CMap(CString, CString))
      .bind("Map[String,String]$$Constructor", Function("Map.Local[String, String]", List(CUnit), CMap(CString, CString)))
      .bind("Map[String,String]&", Pointer(CMap(CString, CString)))
      .bind("Map[String,String]&$$Constructor", Function("Map.Remote[String, String]", List(CString), Pointer(CMap(CString, CString))))

      // Math.
      .bind("abs", Function("abs", List(CDouble), CDouble))
      .bind("acos", Function("acos", List(CDouble), CDouble))
      .bind("acot", Function("acot", List(CDouble), CDouble))
      .bind("acsc", Function("acsc", List(CDouble), CDouble))
      .bind("asec", Function("asec", List(CDouble), CDouble))
      .bind("asin", Function("asin", List(CDouble), CDouble))
      .bind("atan", Function("atan", List(CDouble), CDouble))
      .bind("cbrt", Function("cbrt", List(CDouble), CDouble))
      .bind("ceil", Function("ceil", List(CDouble), CInt))
      .bind("cos", Function("cos", List(CDouble), CDouble))
      .bind("cosh", Function("cosh", List(CDouble), CDouble))
      .bind("cot", Function("cot", List(CDouble), CDouble))
      .bind("coth", Function("coth", List(CDouble), CDouble))
      .bind("csc", Function("csc", List(CDouble), CDouble))
      .bind("csch", Function("csch", List(CDouble), CDouble))
      .bind("exp", Function("exp", List(CDouble), CDouble))
      .bind("expm1", Function("expm1", List(CDouble), CDouble))
      .bind("floor", Function("floor", List(CDouble), CInt))
      .bind("hypot", Function("hypot", List(CDouble, CDouble), CDouble))
      .bind("log", Function("log", List(CDouble), CDouble))
      .bind("log10", Function("log10", List(CDouble), CDouble))
      .bind("log1p", Function("log1p", List(CDouble), CDouble))
      .bind("pow", Function("pow", List(CDouble, CDouble), CDouble))
      .bind("random", Function("random", List(), CDouble))
      .bind("rint", Function("rint", List(CDouble), CInt))
      .bind("round", Function("round", List(CDouble), CInt))
      .bind("sec", Function("sec", List(CDouble), CDouble))
      .bind("sech", Function("sech", List(CDouble), CDouble))
      .bind("signum", Function("signum", List(CDouble), CDouble))
      .bind("sin", Function("sin", List(CDouble), CDouble))
      .bind("sinh", Function("sinh", List(CDouble), CDouble))
      .bind("sqrt", Function("sqrt", List(CDouble), CDouble))
      .bind("tan", Function("tan", List(CDouble), CDouble))
      .bind("tanh", Function("tanh", List(CDouble), CDouble))
  }

}