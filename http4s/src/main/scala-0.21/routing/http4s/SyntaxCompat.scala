package routing
package http4s

import cats.{Applicative, Defer}
import org.{http4s => h}

private[http4s] trait SyntaxCompat { _: SyntaxCompatShape =>
  def uriToHttp4s(uri: ReverseUri): h.Uri =
    h.Uri(path = uri.path, query = queryToHttp4s(uri.query))

  type HttpRoutesEv[F[_]] = util.DualEv[Applicative, Defer, F]

  private[http4s] def mkHttpRoutes[F[_]: HttpRoutesEv](handlers: List[Handled[h.Request[F] => F[h.Response[F]]]]): h.HttpRoutes[F] =
    h.HttpRoutes[F](tryRoutes(_, handlers))
}
