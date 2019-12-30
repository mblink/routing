package org.http4s

import routing.Route
import routing.util.{/~\, Nestable}
import scala.language.implicitConversions

trait RoutingLP {
  implicit class RouteOps[FP, NP](val route: Route.Parameterized[NP])(implicit n: Nestable[FP, NP]) {
    def handled[F[_]](f: FP => F[Response[F]]): Route.HandledRoute[F, route.type] =
      new Route.HandledRoute[F, route.type](route) {
        lazy val handle = /~\[Nestable[?, route.Params], ? => F[Response[F]], FP](n, f)
      }
  }
}

package object routing extends RoutingLP {
  implicit def methodToRoute(m: Method): Route.Aux[Unit, Unit, Unit] = Route.empty(m)

  def queryParam[A](key: String): (String, Option[A]) = key -> None
  def optionalQueryParam[A](key: String): (String, Option[Option[A]]) = key -> None
}
