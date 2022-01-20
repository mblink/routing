package routing
package http4s

import cats.Applicative
import cats.Defer // 0.22, 1.0.0-M10
import cats.Monad // 0.23, 1.0.0-M30
import cats.data.OptionT
import org.{http4s => h}
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
      h.Uri(path = h.Uri.Path.unsafeFromString(uri.path), query = new Http4sReverseQueryOps(uri.query).toHttp4s) // 0.22, 0.23, 1.0.0-M30
      h.Uri(path = h.Uri.Path.fromString(uri.path), query = new Http4sReverseQueryOps(uri.query).toHttp4s) // 1.0.0-M10
  }

  @tailrec
  private[http4s] def tryRoutes[F[_]: Applicative](
    request: h.Request[F],
    handlers: List[Handled[h.Request[F] => F[h.Response[F]]]]
  ): OptionT[F, h.Response[F]] =
    handlers match {
      case Nil => OptionT.none[F, h.Response[F]]
      case handler :: rest =>
        handler.route.unapply(request) match {
          case Some(params) => OptionT.liftF(handler.handle(params)(request))
          case None => tryRoutes(request, rest)
        }
    }

  implicit class Http4sRouteObjectOps(private val route: Route.type) extends AnyVal {
    def httpRoutes[F[_]: Applicative: Defer]( // 0.22, 1.0.0-M10
    def httpRoutes[F[_]: Monad]( // 0.23, 1.0.0-M30
      handlers: Handled[h.Request[F] => F[h.Response[F]]]*
    ): h.HttpRoutes[F] =
      h.HttpRoutes[F](tryRoutes(_, handlers.toList))
  }
}
