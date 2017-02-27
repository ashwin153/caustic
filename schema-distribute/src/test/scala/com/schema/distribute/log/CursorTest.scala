package com.schema.distribute

import com.schema.distribute
import org.scalatest.{AsyncFunSuite, BeforeAndAfter}
import org.scalatest.mockito.MockitoSugar
import scala.concurrent.Future
import org.mockito.Mockito._

class CursorTest extends AsyncFunSuite with MockitoSugar with BeforeAndAfter {

  var cursor: distribute.Cursor[Int] = _

  before {
    this.cursor = spy(classOf[distribute.Cursor[Int]])
    when(this.cursor.next())
      .thenReturn(Future(distribute.Record(0, 0)))
      .thenReturn(Future(distribute.Record(1, 1)))
      .thenReturn(Future(distribute.Pending[Int](2)))
      .thenReturn(Future(distribute.Record(2, 2)))
      .thenReturn(Future(distribute.Pending[Int](3)))
  }

  test("Advance should return n records.") {
    this.cursor.advance(3).map(r => assert(r.map(_.lsn) == Seq(0, 1, 2)))
  }

  test("Advance should return all records up to the first that satisfies the predicate.") {
    this.cursor.advance(_.lsn >= 1).map(r => assert(r.map(_.lsn) == Seq(0, 1)))
  }

  test("Advance should return all records up to the first pending entry.") {
    this.cursor.advance()
      .map(r => assert(r.map(_.lsn) == Seq(0, 1)))
      .flatMap(_ => this.cursor.advance())
      .map(r => assert(r.map(_.lsn) == Seq(2)))
  }

  test("Transform should be applied wherever the transformation is defined.") {
    this.cursor.transform { case Record(i, v) if i % 2 == 0 => "" + v }
      .advance(2)
      .map(r => assert(r.map(_.lsn) == Seq(0, 2)))
  }

}