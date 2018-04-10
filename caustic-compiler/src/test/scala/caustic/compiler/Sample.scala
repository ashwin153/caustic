package caustic.example

import caustic.library.collection._
import caustic.library.control._
import caustic.library.record._
import caustic.library.typing._
import caustic.library.typing.Value._
import caustic.runtime._

import spray.json._
import DefaultJsonProtocol._

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Try



case class Total$Repr(
  count: Int
)

case class Total$Internal(
  count: Value[Int]
)

object Total$Internal {

  implicit def asRef(x: Total$Internal)(
    implicit context: Context
  ): Reference[Total$Repr] = {
    val ref = Reference[Total$Repr](Variable.Local(context.label()))
    ref.get('count) := x.count
    ref
  }

}

case class Total(
  count: scala.Int
)

object Total {

  implicit def asRef(x: Total)(implicit context: Context): Reference[Total$Repr] = {
    val ref = Reference[Total$Repr](Variable.Local(context.label()))
    ref.get('count) := x.count
    ref
  }

  implicit object Total$Format extends RootJsonFormat[Total] {

    def write(x: Total): JsValue = JsObject(
      "count" -> x.count.toJson
    )

    def read(x: JsValue): Total = {
      x.asJsObject.getFields("count") match {
        case Seq(count) =>
          Total(count.convertTo[scala.Int])
        case _ => throw DeserializationException("Total expected, but not found.")
      }
    }

  }


}

import Total._

case class Counter(runtime: Runtime) {
  def increment$Internal(
    x: Reference[Total$Repr]
  )(
    implicit context: Context
  ): Value[Int] = {
    If (x.get('count) === None) {
      x.get('count) := 1
    } Else {
      x.get('count) += 1
    }
    x.get('count)
  }

  def increment(
    x: Total
  ): Try[scala.Int] = {
    this.runtime execute { implicit context: Context =>
      Return(increment$Internal(x).asJson)
    } map {
      case Text(x) => x
      case Real(x) => x.toString
      case Flag(x) => x.toString
      case Null => "null"
    } map {
      _.parseJson.convertTo[scala.Int]
    }
  }

  def incrementTwice$Internal(
    x: Reference[Total$Repr]
  )(
    implicit context: Context
  ): Reference[Total$Repr] = {
    increment$Internal(x)
    increment$Internal(x)
    x
  }

  def incrementTwice(
    x: Total
  ): Try[Total] = {
    this.runtime execute { implicit context: Context =>
      Return(incrementTwice$Internal(x).asJson)
    } map {
      case Text(x) => x
      case Real(x) => x.toString
      case Flag(x) => x.toString
      case Null => "null"
    } map {
      _.parseJson.convertTo[Total]
    }
  }

}