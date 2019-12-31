package org.http4s

import routing.Route
import routing.util.{/~\, Nestable}
import scala.language.implicitConversions

trait RoutingLP {
  implicit class RouteOps[M <: Method, FP, NP](val route: Route[M, NP])(implicit n: Nestable[FP, NP]) {
    def handled[F[_]](f: FP => F[Response[F]]): Route.Handled[F, route.type] =
      new Route.Handled[F, route.type](route) {
        lazy val handle = /~\[Nestable[?, route.Params], ? => F[Response[F]], FP](n, f)
      }
  }
}

package object routing extends RoutingLP {
  type QPMap = Map[String, collection.Seq[String]]

  implicit def methodToRoute[M <: Method](m: M): Route.Aux[M, Unit, Unit, Unit] = Route.empty(m)

  def root[M <: Method](m: M): Route.Aux[M, Unit, Unit, Unit] = Route.empty(m)

  def queryParam[A](key: String): (String, Option[A]) = key -> None
  def optionalQueryParam[A](key: String): (String, Option[Option[A]]) = key -> None
}
