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
      def apply[A](path: DslPath, extract: PathExtractor[A]): Option[(A, DslPath)] =
        path match {
          case s /: rest => extract.extract(s).map(_ -> rest)
          case _ => None
        }

      def rest(path: DslPath): Option[(String, DslPath)] = {
        def go(p: DslPath): List[String] = p match {
          case s /: rest => s :: go(rest)
          case _ => Nil
        }

        Some(go(path)).filter(_.nonEmpty).map(l => l.mkString("/") -> DslRoot)
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
  implicit def toHttp4sRouteOps[M <: Method, P, R <: Route[M, P]](route: R): syntax.Http4sRouteOps[M, P] = new syntax.Http4sRouteOps[M, P](route)
  implicit def toHttp4sRouteObjectOps(route: Route.type): syntax.Http4sRouteObjectOps = new syntax.Http4sRouteObjectOps(route)

  implicit def http4sGETToRoute(m: Http4sMethod.GET.type): Route.Aux[Method.GET.type, Unit, Unit, Unit] = Route.root(Method.GET)
  implicit def http4sPOSTToRoute(m: Http4sMethod.POST.type): Route.Aux[Method.POST.type, Unit, Unit, Unit] = Route.root(Method.POST)
  implicit def http4sPUTToRoute(m: Http4sMethod.PUT.type): Route.Aux[Method.PUT.type, Unit, Unit, Unit] = Route.root(Method.PUT)
  implicit def http4sDELETEToRoute(m: Http4sMethod.DELETE.type): Route.Aux[Method.DELETE.type, Unit, Unit, Unit] = Route.root(Method.DELETE)
  implicit def http4sPATCHToRoute(m: Http4sMethod.PATCH.type): Route.Aux[Method.PATCH.type, Unit, Unit, Unit] = Route.root(Method.PATCH)
  implicit def http4sOPTIONSToRoute(m: Http4sMethod.OPTIONS.type): Route.Aux[Method.OPTIONS.type, Unit, Unit, Unit] = Route.root(Method.OPTIONS)
  implicit def http4sHEADToRoute(m: Http4sMethod.HEAD.type): Route.Aux[Method.HEAD.type, Unit, Unit, Unit] = Route.root(Method.HEAD)

  def root(m: Http4sMethod.GET.type): Route.Aux[Method.GET.type, Unit, Unit, Unit] = Route.root(Method.GET)
  def root(m: Http4sMethod.POST.type)(implicit d: Dummy1): Route.Aux[Method.POST.type, Unit, Unit, Unit] = Route.root(Method.POST)
  def root(m: Http4sMethod.PUT.type)(implicit d: Dummy2): Route.Aux[Method.PUT.type, Unit, Unit, Unit] = Route.root(Method.PUT)
  def root(m: Http4sMethod.DELETE.type)(implicit d: Dummy3): Route.Aux[Method.DELETE.type, Unit, Unit, Unit] = Route.root(Method.DELETE)
  def root(m: Http4sMethod.PATCH.type)(implicit d: Dummy4): Route.Aux[Method.PATCH.type, Unit, Unit, Unit] = Route.root(Method.PATCH)
  def root(m: Http4sMethod.OPTIONS.type)(implicit d: Dummy5): Route.Aux[Method.OPTIONS.type, Unit, Unit, Unit] = Route.root(Method.OPTIONS)
  def root(m: Http4sMethod.HEAD.type)(implicit d: Dummy6): Route.Aux[Method.HEAD.type, Unit, Unit, Unit] = Route.root(Method.HEAD)
}
