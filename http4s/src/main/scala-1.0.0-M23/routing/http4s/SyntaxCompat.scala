package routing
package http4s

import cats.Monad
import org.{http4s => h}

private[http4s] trait SyntaxCompat { _: SyntaxCompatShape =>
  def uriToHttp4s(uri: ReverseUri): h.Uri =
    h.Uri(path = h.Uri.Path.unsafeFromString(uri.path), query = queryToHttp4s(uri.query))

  type HttpRoutesEv[F[_]] = Monad[F]

  private[http4s] def mkHttpRoutes[F[_]: HttpRoutesEv](handlers: List[Handled[h.Request[F] => F[h.Response[F]]]]): h.HttpRoutes[F] =
    h.HttpRoutes[F](tryRoutes(_, handlers))
}
