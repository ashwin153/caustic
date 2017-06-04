package schema.runtime
package syntax

import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncFunSuite, Matchers}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatest.junit.JUnitRunner
import schema.runtime.local.SynchronizedDatabase
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class LanguageTest extends AsyncFunSuite
  with ScalaFutures
  with MockitoSugar
  with Matchers {

  test("Stitch is correctly generated.") {
    val db = spy(SynchronizedDatabase.empty)

    db.execute {
      Schema { implicit ctx =>
        val x = Select("id")
        x.bar = -3.0
        x.foo(3) = "Hello"
        x.foo("baz") = "Goodbye"
        Return(Stitch(x))
      }
    } map { r =>
      verify(db, times(3)).get(any())(any())
      verify(db, times(1)).put(any(), any())(any())
      r shouldEqual """{"key":"id","bar":"-3.0","foo":["3.0":"Hello","baz":"Goodbye"]}"""
    }
  }

  test("Delete is correctly generated.") {
    val db = spy(SynchronizedDatabase.empty)

    db.execute {
      Schema { implicit ctx =>
        val x = Select("id")
        x.bar = -3.0
        x.foo(3) = "Hello"
        x.foo("baz") = "Goodbye"
        Delete(x)
      }
    } flatMap { r =>
      val arg: ArgumentCaptor[Set[String]] = ArgumentCaptor.forClass(classOf[Set[String]])
      verify(db, times(3)).get(arg.capture())(any())
      verify(db, times(1)).put(any(), any())(any())
      r shouldEqual ""

      // Verify that all keys have empty values.
      db.get(arg.getAllValues.asScala.flatten.toSet)
        .map(_.values.filter(_._2.nonEmpty) shouldBe empty)
    }
  }

  test("While is correctly generated.") {
    val db = spy(SynchronizedDatabase.empty)

    db.execute {
      Schema { implicit ctx =>
        val x = Select("id")
        x.bar = -3.0

        While(x.bar < 0) {
          x.bar = x.bar + 1
        }

        Return(x.bar)
      }
    } map { r =>
      verify(db, times(1)).get(any())(any())
      verify(db, times(1)).put(any(), any())(any())
      r shouldEqual "0.0"
    }
  }

  test("For is correctly generated.") {
    implicit val db = spy(SynchronizedDatabase.empty)

    db.execute {
      Schema { implicit ctx =>
        val x = Select("id")
        x.bar = 15.0

        For(ctx.i, 1 to 5) {
          x.bar -= ctx.i
        }

        Return(x.bar)
      }
    } map { r =>
      verify(db, times(1)).get(any())(any())
      verify(db, times(1)).put(any(), any())(any())
      r shouldEqual "0.0"
    }
  }

  test("Foreach is correctly generated.") {
    implicit val db = spy(SynchronizedDatabase.empty)

    db.execute {
      Schema { implicit ctx =>
        val x = Select("id")
        x.foo(3) = "Hello"
        x.foo("bar") = "Goodbye"
        x.foo("baz") = "Stuff"
      }
    } flatMap { _ =>
      // Verify that index addresses are prefetched.
      reset(db)

      db.execute {
        Schema { implicit ctx =>
          val x = Select("id")
          Foreach(ctx.i, x.foo) {
            x.foo(ctx.i) = "Hello"
          }
        }
      } map { _ =>
        verify(db, times(2)).get(any())(any())
        verify(db, times(1)).put(any(), any())(any())
        succeed
      }
    }
  }

}
