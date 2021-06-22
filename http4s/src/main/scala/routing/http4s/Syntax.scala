package routing
package http4s

import cats.Applicative
import cats.data.OptionT
import org.{http4s => h}
import scala.annotation.tailrec

private[http4s] trait SyntaxCompatShape extends util.EvFromDualEv {
  private[http4s] def queryToHttp4s(query: ReverseQuery): h.Query

  private[http4s] def tryRoutes[F[_]: Applicative](
    request: h.Request[F],
    handlers: List[Handled[h.Request[F] => F[h.Response[F]]]]
  ): OptionT[F, h.Response[F]]
}

object syntax extends SyntaxCompatShape with SyntaxCompat {
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

  private[http4s] def queryToHttp4s(query: ReverseQuery): h.Query = new Http4sReverseQueryOps(query).toHttp4s

  implicit class Http4sReverseUriOps(val uri: ReverseUri) extends AnyVal {
    def toHttp4s: h.Uri = uriToHttp4s(uri)
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
    def httpRoutes[F[_]: HttpRoutesEv](handlers: Handled[h.Request[F] => F[h.Response[F]]]*): h.HttpRoutes[F] =
      mkHttpRoutes[F](handlers.toList)
  }
}
