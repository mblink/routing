package routing

import extractor._
import org.http4s.{Method => Http4sMethod, Request => Http4sRequest}
import org.http4s.dsl.impl.{:?, /:, +&, ->, Path => DslPath, Root => DslRoot}
import routing.util.dummy._

package object http4s {
  implicit def http4sDestructuredRequest[F[_]](r: Http4sRequest[F]): DestructuredRequest.Aux[Http4sRequest[F], DslPath, QueryMap] =
    new DestructuredRequest {
      type Request = Http4sRequest[F]
      type ForwardPath = DslPath
      type ForwardQuery = QueryMap

      lazy val request = r
      lazy val parts = r match { case m -> p :? q => Method.fromString(m.name).map((_, p, q)) }
    }

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

  implicit def toHttp4sMethodOps(method: Method): syntax.Http4sMethodOps = new syntax.Http4sMethodOps(method)
  implicit def toHttp4sReverseQueryOps(query: ReverseQuery): syntax.Http4sReverseQueryOps = new syntax.Http4sReverseQueryOps(query)
  implicit def toHttp4sReverseUriOps(uri: ReverseUri): syntax.Http4sReverseUriOps = new syntax.Http4sReverseUriOps(uri)
  implicit def toHttp4sRouteOps[M <: Method, P, R <: Route[M, P]](route: R): syntax.Http4sRouteOps[M, P] = new syntax.Http4sRouteOps[M, P](route)
  implicit def toHttp4sRouteObjectOps(route: Route.type): syntax.Http4sRouteObjectOps = new syntax.Http4sRouteObjectOps(route)

  implicit def http4sGETToRoute(@unused m: Http4sMethod.GET.type): Route.Aux[Method.GET.type, Unit, Unit, Unit] = Route.empty(Method.GET)
  implicit def http4sPOSTToRoute(@unused m: Http4sMethod.POST.type): Route.Aux[Method.POST.type, Unit, Unit, Unit] = Route.empty(Method.POST)
  implicit def http4sPUTToRoute(@unused m: Http4sMethod.PUT.type): Route.Aux[Method.PUT.type, Unit, Unit, Unit] = Route.empty(Method.PUT)
  implicit def http4sDELETEToRoute(@unused m: Http4sMethod.DELETE.type): Route.Aux[Method.DELETE.type, Unit, Unit, Unit] = Route.empty(Method.DELETE)
  implicit def http4sPATCHToRoute(@unused m: Http4sMethod.PATCH.type): Route.Aux[Method.PATCH.type, Unit, Unit, Unit] = Route.empty(Method.PATCH)
  implicit def http4sOPTIONSToRoute(@unused m: Http4sMethod.OPTIONS.type): Route.Aux[Method.OPTIONS.type, Unit, Unit, Unit] = Route.empty(Method.OPTIONS)
  implicit def http4sHEADToRoute(@unused m: Http4sMethod.HEAD.type): Route.Aux[Method.HEAD.type, Unit, Unit, Unit] = Route.empty(Method.HEAD)

  def root(@unused m: Http4sMethod.GET.type): Route.Aux[Method.GET.type, Unit, Unit, Unit] = Route.empty(Method.GET)
  def root(@unused m: Http4sMethod.POST.type)(implicit @unused d: Dummy1): Route.Aux[Method.POST.type, Unit, Unit, Unit] = Route.empty(Method.POST)
  def root(@unused m: Http4sMethod.PUT.type)(implicit @unused d: Dummy2): Route.Aux[Method.PUT.type, Unit, Unit, Unit] = Route.empty(Method.PUT)
  def root(@unused m: Http4sMethod.DELETE.type)(implicit @unused d: Dummy3): Route.Aux[Method.DELETE.type, Unit, Unit, Unit] = Route.empty(Method.DELETE)
  def root(@unused m: Http4sMethod.PATCH.type)(implicit @unused d: Dummy4): Route.Aux[Method.PATCH.type, Unit, Unit, Unit] = Route.empty(Method.PATCH)
  def root(@unused m: Http4sMethod.OPTIONS.type)(implicit @unused d: Dummy5): Route.Aux[Method.OPTIONS.type, Unit, Unit, Unit] = Route.empty(Method.OPTIONS)
  def root(@unused m: Http4sMethod.HEAD.type)(implicit @unused d: Dummy6): Route.Aux[Method.HEAD.type, Unit, Unit, Unit] = Route.empty(Method.HEAD)
}
