package routing
package http4s

import cats.{Applicative, Defer}
import cats.data.OptionT
import org.{http4s => h}
import routing.util.Nestable
import scala.annotation.tailrec

object syntax {
  implicit class Http4sMethodOps(val method: Method) extends AnyVal {
    def toHttp4s: h.Method = method match {
      case Method.GET => h.Method.GET
      case Method.POST => h.Method.POST
      case Method.PUT => h.Method.PUT
      case Method.DELETE => h.Method.DELETE
      case Method.PATCH => h.Method.PATCH
      case Method.OPTIONS => h.Method.OPTIONS
      case Method.HEAD => h.Method.HEAD
    }
  }

  implicit class Http4sReverseQueryOps(val query: ReverseQuery) extends AnyVal {
    def toHttp4s: h.Query = h.Query.fromVector(query)
  }

  implicit class Http4sReverseUriOps(val uri: ReverseUri) extends AnyVal {
    def toHttp4s: h.Uri =
      h.Uri(path = uri.path, query = (new Http4sReverseQueryOps(uri.query)).toHttp4s)
  }

  implicit class Http4sRouteOps[M <: Method, P](val route: Route[M, P]) extends AnyVal {
    def unapply[F[_], A](request: h.Request[F])(implicit N: Nestable[A, P]): Option[A] =
      route.unapplyNested(request).map(N.unnest(_))
  }

  @tailrec
  private def tryRoutes[F[_]: Applicative](
    request: h.Request[F],
    handlers: collection.Seq[Handled[h.Request[F] => F[h.Response[F]]]]
  ): OptionT[F, h.Response[F]] =
    handlers match {
      case Seq() => OptionT.none[F, h.Response[F]]
      case handler +: rest =>
        handler.route.unapplyNested(request) match {
          case Some(params) => OptionT.liftF(handler.handleNested(params)(request))
          case None => tryRoutes(request, rest)
        }
    }

  trait MkHttpRoutes {
    def apply[F[_]: Applicative: Defer](handlers: Handled[h.Request[F] => F[h.Response[F]]]*): h.HttpRoutes[F] =
      h.HttpRoutes[F](tryRoutes(_, handlers))
  }

  implicit class Http4sRouteObjectOps(val route: Route.type) extends AnyVal {
    def httpRoutes: MkHttpRoutes = new MkHttpRoutes {}
  }
}
