package caustic.distribute

import org.junit.runner.RunWith
import org.scalatest.{AsyncFunSuite, BeforeAndAfter}
import org.scalatest.mockito.MockitoSugar
import scala.concurrent.Future
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CursorTest extends AsyncFunSuite
  with MockitoSugar
  with BeforeAndAfter {

  var cursor: Cursor[Int] = _

  before {
    this.cursor = spy(classOf[Cursor[Int]])
    when(this.cursor.next())
      .thenReturn(Future(Record(0, 0)))
      .thenReturn(Future(Record(1, 1)))
      .thenReturn(Future(Pending[Int](2)))
      .thenReturn(Future(Record(2, 2)))
      .thenReturn(Future(Pending[Int](3)))
  }

  test("Advance should return n records.") {
    this.cursor.advance(3).map(r => assert(r.map(_.offset) == Seq(0, 1, 2)))
  }

  test("Advance should return all records up to the first that satisfies the predicate.") {
    this.cursor.advance(_.offset >= 1).map(r => assert(r.map(_.offset) == Seq(0, 1)))
  }

  test("Advance should return all records up to the first pending entry.") {
    this.cursor.advance()
      .map(r => assert(r.map(_.offset) == Seq(0, 1)))
      .flatMap(_ => this.cursor.advance())
      .map(r => assert(r.map(_.offset) == Seq(2)))
  }

  test("Transform should be applied wherever the transformation is defined.") {
    this.cursor.transform { case Record(i, v) if i % 2 == 0 => "" + v }
      .advance(2)
      .map(r => assert(r.map(_.offset) == Seq(0, 2)))
  }

}