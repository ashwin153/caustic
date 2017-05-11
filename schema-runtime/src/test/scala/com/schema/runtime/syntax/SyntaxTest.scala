package com.schema.runtime.syntax

import com.schema.runtime._
import org.scalatest.FunSuite
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class SyntaxTest extends FunSuite {

  implicit val db = local.SynchronizedDatabase.empty

  test("Syntax Test") {
    println(Await.result(Schema { implicit ctx =>
      val x = Select("id")
      x.foo = "Hello"
      x.grades("math") = 4.0
      Return (x)
    }, Duration.Inf))
  }

}
