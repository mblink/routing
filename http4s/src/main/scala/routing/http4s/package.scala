package routing

import extractor._
import org.http4s.{Method => Http4sMethod, Request => Http4sRequest}
import org.http4s.dsl.impl.{:?, /:, +&, ->}
import org.http4s.Uri.{Path => Http4sPath}
import routing.util.dummy._

package object http4s {
  private val rootHttp4sPath: Http4sPath = Http4sPath.Root
  private def http4sPathIsEmpty(path: Http4sPath): Boolean = path.isEmpty

  implicit val http4sRootPath: RootPath[Http4sPath] =
    new RootPath[Http4sPath] {
      def apply(): Http4sPath = rootHttp4sPath
    }

  implicit val http4sExtractPathPart: ExtractPathPart[Http4sPath] =
    new ExtractPathPart[Http4sPath] {
      private def normalizePath(path: Http4sPath): Http4sPath =
        if (http4sPathIsEmpty(path)) rootHttp4sPath else path

      val rootPath: RootPath[Http4sPath] = http4sRootPath

      def apply[A](path: Http4sPath, extract: PathExtractor[A]): Option[(A, Http4sPath)] =
        path match {
          case s /: rest => extract.extract(s).map(_ -> normalizePath(rest))
          case _ => None
        }

      def apply[A](path: Http4sPath, extract: RestOfPathExtractor[A]): Option[A] = {
        def go(p: Http4sPath): List[String] = normalizePath(p) match {
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

  implicit def http4sExtractRequest[F[_]]: ExtractRequest.Aux[Http4sRequest[F], Http4sPath, QueryMap] =
    new ExtractRequest[Http4sRequest[F]] {
      type ForwardPath = Http4sPath
      type ForwardQuery = QueryMap

      def parts(request: Http4sRequest[F]): Option[(Method, Http4sPath, QueryMap)] =
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

  implicit def http4sGETToRoute(@annotation.nowarn("msg=never used") m: Http4sMethod.GET.type): Route[Method.GET.type, Unit] = Route.root(Method.GET)
  implicit def http4sPOSTToRoute(@annotation.nowarn("msg=never used") m: Http4sMethod.POST.type): Route[Method.POST.type, Unit] = Route.root(Method.POST)
  implicit def http4sPUTToRoute(@annotation.nowarn("msg=never used") m: Http4sMethod.PUT.type): Route[Method.PUT.type, Unit] = Route.root(Method.PUT)
  implicit def http4sDELETEToRoute(@annotation.nowarn("msg=never used") m: Http4sMethod.DELETE.type): Route[Method.DELETE.type, Unit] = Route.root(Method.DELETE)
  implicit def http4sPATCHToRoute(@annotation.nowarn("msg=never used") m: Http4sMethod.PATCH.type): Route[Method.PATCH.type, Unit] = Route.root(Method.PATCH)
  implicit def http4sOPTIONSToRoute(@annotation.nowarn("msg=never used") m: Http4sMethod.OPTIONS.type): Route[Method.OPTIONS.type, Unit] = Route.root(Method.OPTIONS)
  implicit def http4sHEADToRoute(@annotation.nowarn("msg=never used") m: Http4sMethod.HEAD.type): Route[Method.HEAD.type, Unit] = Route.root(Method.HEAD)

  def root(@annotation.nowarn("msg=never used") m: Http4sMethod.GET.type): Route[Method.GET.type, Unit] = Route.root(Method.GET)
  def root(@annotation.nowarn("msg=never used") m: Http4sMethod.POST.type)(implicit @annotation.nowarn("msg=never used") d: Dummy1): Route[Method.POST.type, Unit] = Route.root(Method.POST)
  def root(@annotation.nowarn("msg=never used") m: Http4sMethod.PUT.type)(implicit @annotation.nowarn("msg=never used") d: Dummy2): Route[Method.PUT.type, Unit] = Route.root(Method.PUT)
  def root(@annotation.nowarn("msg=never used") m: Http4sMethod.DELETE.type)(implicit @annotation.nowarn("msg=never used") d: Dummy3): Route[Method.DELETE.type, Unit] = Route.root(Method.DELETE)
  def root(@annotation.nowarn("msg=never used") m: Http4sMethod.PATCH.type)(implicit @annotation.nowarn("msg=never used") d: Dummy4): Route[Method.PATCH.type, Unit] = Route.root(Method.PATCH)
  def root(@annotation.nowarn("msg=never used") m: Http4sMethod.OPTIONS.type)(implicit @annotation.nowarn("msg=never used") d: Dummy5): Route[Method.OPTIONS.type, Unit] = Route.root(Method.OPTIONS)
  def root(@annotation.nowarn("msg=never used") m: Http4sMethod.HEAD.type)(implicit @annotation.nowarn("msg=never used") d: Dummy6): Route[Method.HEAD.type, Unit] = Route.root(Method.HEAD)
}
