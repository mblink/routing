package routing

import extractor._
import org.http4s.{Method => Http4sMethod, Request => Http4sRequest}
import org.http4s.dsl.impl.{:?, /:, +&, ->, Path => DslPath, Root => DslRoot}
import routing.util.dummy._

package object http4s {
  implicit val http4sRootPath: RootPath[DslPath] =
    new RootPath[DslPath] {
      def apply(): DslPath = DslRoot
    }

  implicit val http4sExtractPathPart: ExtractPathPart[DslPath] =
    new ExtractPathPart[DslPath] {
      private def normalizePath(path: DslPath): DslPath =
        if (path.toList.isEmpty) DslRoot else path

      val rootPath: RootPath[DslPath] = http4sRootPath

      def apply[A](path: DslPath, extract: PathExtractor[A]): Option[(A, DslPath)] =
        path match {
          case s /: rest => extract.extract(s).map(_ -> normalizePath(rest))
          case _ => None
        }

      def apply[A](path: DslPath, extract: RestOfPathExtractor[A]): Option[A] = {
        def go(p: DslPath): List[String] = normalizePath(p) match {
          case s /: rest => s :: go(rest)
          case _ => Nil
        }

        Some(go(path)).filter(_.nonEmpty).map(_.mkString("/")).flatMap(extract.extract)
      }
    }

  implicit val http4sExtractQueryPart: ExtractQueryPart[QueryMap] =
    new ExtractQueryPart[QueryMap] {
      def apply[A](query: QueryMap, key: String, extract: QueryExtractor[A]): Option[(A, QueryMap)] =
        query match {
          case m +& rest => extract.extract(key, m).map(_ -> rest)
        }
    }

  implicit def http4sExtractRequest[F[_]]: ExtractRequest.Aux[Http4sRequest[F], DslPath, QueryMap] =
    new ExtractRequest[Http4sRequest[F]] {
      type ForwardPath = DslPath
      type ForwardQuery = QueryMap

      def parts(request: Http4sRequest[F]): Option[(Method, DslPath, QueryMap)] =
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
  implicit def toHttp4sRouteObjectOps(route: Route.type): syntax.Http4sRouteObjectOps = new syntax.Http4sRouteObjectOps(route)

  implicit def http4sGETToRoute(@uu m: Http4sMethod.GET.type): Route[Method.GET.type, Unit] = Route.root(Method.GET)
  implicit def http4sPOSTToRoute(@uu m: Http4sMethod.POST.type): Route[Method.POST.type, Unit] = Route.root(Method.POST)
  implicit def http4sPUTToRoute(@uu m: Http4sMethod.PUT.type): Route[Method.PUT.type, Unit] = Route.root(Method.PUT)
  implicit def http4sDELETEToRoute(@uu m: Http4sMethod.DELETE.type): Route[Method.DELETE.type, Unit] = Route.root(Method.DELETE)
  implicit def http4sPATCHToRoute(@uu m: Http4sMethod.PATCH.type): Route[Method.PATCH.type, Unit] = Route.root(Method.PATCH)
  implicit def http4sOPTIONSToRoute(@uu m: Http4sMethod.OPTIONS.type): Route[Method.OPTIONS.type, Unit] = Route.root(Method.OPTIONS)
  implicit def http4sHEADToRoute(@uu m: Http4sMethod.HEAD.type): Route[Method.HEAD.type, Unit] = Route.root(Method.HEAD)

  def root(@uu m: Http4sMethod.GET.type): Route[Method.GET.type, Unit] = Route.root(Method.GET)
  def root(@uu m: Http4sMethod.POST.type)(implicit @uu d: Dummy1): Route[Method.POST.type, Unit] = Route.root(Method.POST)
  def root(@uu m: Http4sMethod.PUT.type)(implicit @uu d: Dummy2): Route[Method.PUT.type, Unit] = Route.root(Method.PUT)
  def root(@uu m: Http4sMethod.DELETE.type)(implicit @uu d: Dummy3): Route[Method.DELETE.type, Unit] = Route.root(Method.DELETE)
  def root(@uu m: Http4sMethod.PATCH.type)(implicit @uu d: Dummy4): Route[Method.PATCH.type, Unit] = Route.root(Method.PATCH)
  def root(@uu m: Http4sMethod.OPTIONS.type)(implicit @uu d: Dummy5): Route[Method.OPTIONS.type, Unit] = Route.root(Method.OPTIONS)
  def root(@uu m: Http4sMethod.HEAD.type)(implicit @uu d: Dummy6): Route[Method.HEAD.type, Unit] = Route.root(Method.HEAD)
}
