package routing

import routing.extractor._
import _root_.play.api.mvc.RequestHeader
import _root_.play.api.routing.sird.{PathExtractor => _, _}

package object play {
  implicit val playRootPath: RootPath[String] =
    new RootPath[String] {
      def apply(): String = "/"
    }

  implicit val playExtractPathPart: ExtractPathPart[String] =
    new ExtractPathPart[String] {
      val rootPath: RootPath[String] = playRootPath

      def apply[A](path: String, extract: PathExtractor[A]): Option[(A, String)] =
        path match {
          case p"/$s" => extract.extract(s).map(_ -> "/")
          case p"/$s/$rest*" => extract.extract(s).map(_ -> s"/$rest")
          case _ => None
        }

      def apply[A](path: String, extract: RestOfPathExtractor[A]): Option[A] =
        path match {
          case p"/$s*" => extract.extract(s)
          case _ => None
        }
    }

  implicit val playExtractQueryPart: ExtractQueryPart[QueryMap] =
    new ExtractQueryPart[QueryMap] {
      def apply[A](query: QueryMap, key: String, extract: QueryExtractor[A]): Option[(A, QueryMap)] =
        extract.extract(key, query).map(_ -> query)
    }

  implicit def playExtractRequest: ExtractRequest.Aux[RequestHeader, String, QueryMap] =
    new ExtractRequest[RequestHeader] {
      type ForwardPath = String
      type ForwardQuery = QueryMap

      def parts(request: RequestHeader): Option[(Method, String, QueryMap)] =
        Method.fromString(request.method).map((_, request.path match {
          case "" => playRootPath()
          case p => p
        }, request.queryString))

      lazy val rootPath = playRootPath
      lazy val extractPath = playExtractPathPart
      lazy val extractQuery = playExtractQueryPart
    }

  implicit def toPlayCallOps(call: Call): syntax.PlayCallOps = new syntax.PlayCallOps(call)
  implicit def toPlayRouteObjectOps(route: Route.type): syntax.PlayRouteObjectOps = new syntax.PlayRouteObjectOps(route)
}
