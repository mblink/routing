package routing

import extractor._
import org.http4s.{Method => Http4sMethod, Uri => Http4sUri, Request => Http4sRequest}
import org.http4s.dsl.impl.{:?, /:, +&, ->}
import routing.util.dummy._

package object http4s {
  implicit val http4sRootPath: RootPath[Http4sUri.Path] =
    new RootPath[Http4sUri.Path] {
      def apply(): Http4sUri.Path = Http4sUri.Path.Root
    }

  implicit val http4sExtractPathPart: ExtractPathPart[Http4sUri.Path] =
    new ExtractPathPart[Http4sUri.Path] {
      private def normalizePath(path: Http4sUri.Path): Http4sUri.Path =
        if (path.isEmpty) Http4sUri.Path.Root else path

      def apply[A](path: Http4sUri.Path, extract: PathExtractor[A]): Option[(A, Http4sUri.Path)] =
        path match {
          case s /: rest => extract.extract(s).map(_ -> normalizePath(rest))
          case _ => None
        }

      def rest(path: Http4sUri.Path): Option[(String, Http4sUri.Path)] = {
        def go(p: Http4sUri.Path): List[String] = normalizePath(p) match {
          case s /: rest => s :: go(rest)
          case _ => Nil
        }

        Some(go(path)).filter(_.nonEmpty).map(l => l.mkString("/") -> Http4sUri.Path.Root)
      }
    }

  implicit val http4sExtractQueryPart: ExtractQueryPart[QueryMap] =
    new ExtractQueryPart[QueryMap] {
      def apply[A](query: QueryMap, key: String, extract: QueryExtractor[A]): Option[(A, QueryMap)] =
        query match {
          case m +& rest => extract.extract(key, m).map(_ -> rest)
        }
    }

  implicit def http4sExtractRequest[F[_]]: ExtractRequest.Aux[Http4sRequest[F], Http4sUri.Path, QueryMap] =
    new ExtractRequest[Http4sRequest[F]] {
      type ForwardPath = Http4sUri.Path
      type ForwardQuery = QueryMap

      def parts(request: Http4sRequest[F]): Option[(Method, Http4sUri.Path, QueryMap)] =
        request match {
          case m -> p :? q => Method.fromString(m.name).map((_, p, q))
        }
      lazy val rootPath = http4sRootPath
      lazy val extractPath = http4sExtractPathPart
      lazy val extractQuery = http4sExtractQueryPart
    }

  implicit def toHttp4sMethodOps(method: Method): syntax.Http4sMethodOps = new syntax.Http4sMethodOps(method)
  implicit def toHttp4sReverseQueryOps(query: ReverseQuery): syntax.Http4sReverseQueryOps = new syntax.Http4sReverseQueryOps(query)
  implicit def toHttp4sReverseUriOps(uri: ReverseUri): syntax.Http4sReverseUriOps = new syntax.Http4sReverseUriOps(uri)
  implicit def toHttp4sRouteOps[M <: Method, P, R <: Route[M, P]](route: R): syntax.Http4sRouteOps[M, P] = new syntax.Http4sRouteOps[M, P](route)
  implicit def toHttp4sRouteObjectOps(route: Route.type): syntax.Http4sRouteObjectOps = new syntax.Http4sRouteObjectOps(route)

  implicit def http4sGETToRoute(@uu m: Http4sMethod.GET.type): Route.Aux[Method.GET.type, Unit, Unit, Unit] = Route.root(Method.GET)
  implicit def http4sPOSTToRoute(@uu m: Http4sMethod.POST.type): Route.Aux[Method.POST.type, Unit, Unit, Unit] = Route.root(Method.POST)
  implicit def http4sPUTToRoute(@uu m: Http4sMethod.PUT.type): Route.Aux[Method.PUT.type, Unit, Unit, Unit] = Route.root(Method.PUT)
  implicit def http4sDELETEToRoute(@uu m: Http4sMethod.DELETE.type): Route.Aux[Method.DELETE.type, Unit, Unit, Unit] = Route.root(Method.DELETE)
  implicit def http4sPATCHToRoute(@uu m: Http4sMethod.PATCH.type): Route.Aux[Method.PATCH.type, Unit, Unit, Unit] = Route.root(Method.PATCH)
  implicit def http4sOPTIONSToRoute(@uu m: Http4sMethod.OPTIONS.type): Route.Aux[Method.OPTIONS.type, Unit, Unit, Unit] = Route.root(Method.OPTIONS)
  implicit def http4sHEADToRoute(@uu m: Http4sMethod.HEAD.type): Route.Aux[Method.HEAD.type, Unit, Unit, Unit] = Route.root(Method.HEAD)

  def root(@uu m: Http4sMethod.GET.type): Route.Aux[Method.GET.type, Unit, Unit, Unit] = Route.root(Method.GET)
  def root(@uu m: Http4sMethod.POST.type)(implicit @uu d: Dummy1): Route.Aux[Method.POST.type, Unit, Unit, Unit] = Route.root(Method.POST)
  def root(@uu m: Http4sMethod.PUT.type)(implicit @uu d: Dummy2): Route.Aux[Method.PUT.type, Unit, Unit, Unit] = Route.root(Method.PUT)
  def root(@uu m: Http4sMethod.DELETE.type)(implicit @uu d: Dummy3): Route.Aux[Method.DELETE.type, Unit, Unit, Unit] = Route.root(Method.DELETE)
  def root(@uu m: Http4sMethod.PATCH.type)(implicit @uu d: Dummy4): Route.Aux[Method.PATCH.type, Unit, Unit, Unit] = Route.root(Method.PATCH)
  def root(@uu m: Http4sMethod.OPTIONS.type)(implicit @uu d: Dummy5): Route.Aux[Method.OPTIONS.type, Unit, Unit, Unit] = Route.root(Method.OPTIONS)
  def root(@uu m: Http4sMethod.HEAD.type)(implicit @uu d: Dummy6): Route.Aux[Method.HEAD.type, Unit, Unit, Unit] = Route.root(Method.HEAD)
}
