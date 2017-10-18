package caustic.benchmarks

import caustic.runtime._
import caustic.runtime.jdbc.JdbcDatabase

import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

object ProfileBenchmark extends App {

  val config = ConfigFactory.load()
  val database: Database = JdbcDatabase(config.getConfig("caustic.runtime.database.jdbc"))
  val transaction: Transaction = Seq.fill(1 << 10)(write(text("x"), text(""))).reduce(cons)
  Await.ready(this.database.execute(this.transaction), 5 seconds)

}
