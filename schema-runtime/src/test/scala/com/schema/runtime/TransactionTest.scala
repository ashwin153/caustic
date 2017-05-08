package com.schema.runtime

import org.scalatest.{FunSuite, Matchers}
import org.scalatest.mockito.MockitoSugar

class TransactionTest extends FunSuite with MockitoSugar with Matchers {

  test("Readset is correctly generated.") {
    read("A").readset should contain theSameElementsAs Set("A")
    write("A", read("B")).readset should contain theSameElementsAs Set("B")
    read(read("A")).readset should contain theSameElementsAs Set("A")
    literal("A").readset should have size 0
    write("A", "").readset should have size 0
  }

  test("Writeset is correctly generated.") {
    write("A", "").writeset should contain theSameElementsAs Set("A")
    write("A", read("B")).writeset should contain theSameElementsAs Set("A")
    literal("A").writeset should have size 0
    read("A").writeset should have size 0
  }

}
