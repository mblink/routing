package routing.example

import org.http4s.dsl.io._
import routing._
import routing.http4s._

object routes {
  val WidgetsIndex = GET / "widgets"
  val WidgetsUpsert = POST / "widgets" / pathVar[Int]("id")
  val WidgetsShow = GET / "widgets" / pathVar[Int]("id")
  val WidgetsDelete = DELETE / "widgets" / pathVar[Int]("id")
}
