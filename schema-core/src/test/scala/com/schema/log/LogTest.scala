package com.schema.log

import org.junit.runner.RunWith
import org.scalatest.{Matchers, fixture}
import org.scalatest.junit.JUnitRunner
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
trait LogTest extends fixture.AsyncFunSuite with Matchers {

  override type FixtureParam = Log[Int]

  test("Append should return monotonically increasing sequence numbers.") { log =>
    log.append(0).flatMap(a => log.append(1).map(b => assert(a.lsn < b.lsn)))
  }

  test("Append should return non-negative sequence numbers.") { log =>
    log.append(0).map(r => assert(r.lsn >= 0))
  }

  test("Read should properly iterate log entries.") { log =>
    Future.sequence(Seq(log.append(0), log.append(1), log.append(2))).flatMap { _ =>
      val cursor = log.read(1)
      cursor.next()
        .map(_ should matchPattern { case Record(_, x) if x == 1 => })
        .flatMap(_ => cursor.next())
        .map(_ should matchPattern { case Record(_, x) if x == 2 => })
        .flatMap(_ => cursor.next())
        .flatMap(a => cursor.next().map(b => assert(a === b && a.isInstanceOf[Pending[Int]])))
    }
  }

}
