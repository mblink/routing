package org.http4s.routing.example

import cats.effect.IO
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.headers.Location

object Controller {
  var widgets = Map[Int, Widget]()

  val actions = HttpRoutes.of[IO] {
    case routes.WidgetsIndex(_) => Ok(widgets.asJson)
    case req @ routes.WidgetsUpsert(id) => req.as[Widget].flatMap { w =>
      widgets = widgets + (id -> w)
      Found(Location(routes.WidgetsIndex.uri()))
    }
    case routes.WidgetsShow(id) => Ok(widgets.get(id).asJson)
    case routes.WidgetsDelete(id) =>
      widgets = widgets - id
      Found(Location(routes.WidgetsIndex.uri()))
  }
}
