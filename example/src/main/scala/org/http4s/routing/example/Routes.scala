package org.http4s.routing.example

import cats.instances.int._
import org.http4s.dsl.io._
import org.http4s.routing._

object routes {
  val WidgetsIndex = GET / "widgets"
  val WidgetsUpsert = POST / "widgets" / ("id" -> IntVar)
  val WidgetsShow = GET / "widgets" / ("id" -> IntVar)
  val WidgetsDelete = DELETE / "widgets" / ("id" -> IntVar)
}
