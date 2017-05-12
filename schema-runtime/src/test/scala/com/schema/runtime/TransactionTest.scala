package com.schema.runtime

import org.scalatest.{FunSuite, Matchers}
import org.scalatest.mockito.MockitoSugar

class TransactionTest extends FunSuite with MockitoSugar with Matchers {

  test("Readset is correctly generated.") {
    read(literal("A")).readset should contain theSameElementsAs Set("A")
    write(literal("A"), read(literal("B"))).readset should contain theSameElementsAs Set("B")
    read(read(literal("A"))).readset should contain theSameElementsAs Set("A")
    write(literal("A"), Literal.True).readset should have size 0
    literal("A").readset should have size 0
  }

  test("Writeset is correctly generated.") {
    write(literal("A"), Literal.True).writeset should contain theSameElementsAs Set("A")
    write(literal("A"), read(literal("B"))).writeset should contain theSameElementsAs Set("A")
    read(literal("A")).writeset should have size 0
    literal("A").writeset should have size 0
  }

}
