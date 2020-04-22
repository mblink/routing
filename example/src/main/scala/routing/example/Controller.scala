package routing.example

import cats.effect.IO
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.headers.Location
import routing.Route
import routing.http4s._

object Controller {
  var widgets = Map[Int, Widget]()

  val actions: HttpRoutes[IO] = Route.httpRoutes.of[IO] {
    case routes.WidgetsIndex(_) => Ok(widgets.asJson)
    case req @ routes.WidgetsUpsert(id) => req.request.as[Widget].flatMap { w =>
      widgets = widgets + (id -> w)
      Found(Location(routes.WidgetsIndex.uri().toHttp4s))
    }
    case routes.WidgetsShow(id) => Ok(widgets.get(id).asJson)
    case routes.WidgetsDelete(id) =>
      widgets = widgets - id
      Found(Location(routes.WidgetsIndex.uri().toHttp4s))
  }
}
