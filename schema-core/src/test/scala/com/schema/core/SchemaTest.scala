package com.schema.core

import com.schema.local.LocalSnapshot
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import shapeless.LabelledGeneric

@RunWith(classOf[JUnitRunner])
class SchemaTest extends FunSuite {

  test("Select, insert, and delete should be strongly consistent.") {
    case class Foo(bar: String)

    val schema = new Schema(LocalSnapshot.empty)

    // Verify that the id doesn't currently exist, and then insert an object.
    assert(schema.select[Foo]("id").isEmpty)
    schema.insert("id", Foo("hello"))

    val gen = LabelledGeneric[Foo]
    val x = gen.to(Foo("hello"))


    // Update the value of the bar field.
    val x = schema.select[Foo]("id").get
    assert(x('bar) == "hello")
    x('bar) = "bye"
    assert(x('bar) == "bye")

    // Verify that future accesses contain updated value.
    val y = schema.select[Foo]("id").get
    assert(y('bar) == "bye")

    // Delete the object and verify that it can no longer be accessed.
    schema.delete("id")
    assert(schema.select[Foo]("id").isEmpty)
  }

}
