package caustic.runtime.parser

import caustic.runtime.thrift
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}

@RunWith(classOf[JUnitRunner])
class TransactionTest extends FunSuite with Matchers {

  test("Thrift literals are correctly parsed.") {
    Transaction.parse(thrift.Transaction.literal(thrift.Literal.flag(true))) shouldEqual flag(true)
    Transaction.parse(thrift.Transaction.literal(thrift.Literal.real(0))) shouldEqual real(0)
    Transaction.parse(thrift.Transaction.literal(thrift.Literal.text("a"))) shouldEqual text("a")
  }

  test("Thrift operations are correctly parsed.") {
    Transaction.parse(
      thrift.Transaction.expression(thrift.Expression.read(new thrift.Read(
        thrift.Transaction.expression(thrift.Expression.add(new thrift.Add(
          thrift.Transaction.literal(thrift.Literal.text("foo")),
          thrift.Transaction.literal(thrift.Literal.text("bar"))
        )))
      )))
    ) shouldEqual read(add(text("foo"), text("bar")))
  }

}